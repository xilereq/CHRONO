package pl.chrono.timemanager.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.chrono.dbmanager.model.Policy;
import pl.chrono.dbmanager.model.User;
import pl.chrono.dbmanager.model.WorkLog;
import pl.chrono.dbmanager.model.WorkLogRequest;
import pl.chrono.dbmanager.model.enums.ApplicationType;
import pl.chrono.dbmanager.model.enums.WorkLocationType;
import pl.chrono.dbmanager.model.enums.WorkLogRequestStatus;
import pl.chrono.dbmanager.ports.I_TimeData;
import pl.chrono.dbmanager.repository.ApplicationRepository;
import pl.chrono.dbmanager.repository.UserRepository;
import pl.chrono.timemanager.dto.request.WorkLogCreateRequest;
import pl.chrono.timemanager.dto.request.WorkLogResolveRequest;
import pl.chrono.timemanager.dto.response.WorkLogRequestDto;
import pl.chrono.timemanager.exception.UserException;
import pl.chrono.timemanager.exception.WorkLogRequestException;
import pl.chrono.timemanager.mapper.WorkLogRequestMapper;

@Service
@RequiredArgsConstructor
public class WorkLogRequestService {

  private final UserRepository userRepository;
  private final I_TimeData timeData;
  private final ApplicationRepository applicationRepository;
  private final WorkLogRequestMapper mapper;

  @Transactional
  public void submitRequest(Integer employeeId, WorkLogCreateRequest requestDto) {
    LocalDateTime proposedStart = requestDto.proposedStartTime();
    LocalDateTime proposedEnd = requestDto.proposedEndTime();

    if (proposedStart.isAfter(proposedEnd) || proposedStart.isEqual(proposedEnd)) {
      throw new WorkLogRequestException.InvalidTimeRange();
    }

    User employee = userRepository.findById(employeeId)
        .orElseThrow(() -> new UserException.NotFound(employeeId));

    Policy policy = employee.getPolicyForDate(proposedStart.toLocalDate());
    if (policy != null) {
      long requestedSeconds = java.time.Duration.between(proposedStart, proposedEnd).getSeconds();
      if (requestedSeconds > policy.getMaxAllowedSeconds()) {
        throw new WorkLogRequestException.DailyLimitExceeded(policy.getMaxDailyHours());
      }
    }

    WorkLogRequest request = new WorkLogRequest();
    request.setEmployee(employee);
    request.setProposedStartTime(proposedStart);
    request.setProposedEndTime(proposedEnd);
    request.setReason(requestDto.reason());
    request.setStatus(WorkLogRequestStatus.PENDING);

    timeData.saveWorkLogRequest(request);
  }

  @Transactional
  public void approveRequest(Long requestId, Integer reviewerId, WorkLogResolveRequest resolveDto) {
    WorkLogRequest request = getPendingRequest(requestId);
    User reviewer = getUserAndValidateAccess(reviewerId, request);

    LocalDate targetDate = request.getProposedStartTime().toLocalDate();
    LocalDateTime startOfDay = targetDate.atStartOfDay();
    LocalDateTime endOfDay = targetDate.plusDays(1).atStartOfDay().minusNanos(1);

    List<WorkLog> existingLogs = timeData.findWorkLogsByUserAndDate(
        request.getEmployee().getUserId(), startOfDay, endOfDay);

    for (WorkLog log : existingLogs) {
      timeData.deleteWorkLog(log);
    }

    WorkLog newWorkLog = new WorkLog();
    newWorkLog.setUser(request.getEmployee());
    newWorkLog.setStartTime(request.getProposedStartTime());
    newWorkLog.setEndTime(request.getProposedEndTime());

    boolean hasHoApplication = applicationRepository.findApprovedByEmployeeAndDate(
            request.getEmployee().getUserId(), targetDate).stream()
        .anyMatch(app -> app.getType() == ApplicationType.HOME_OFFICE);
    newWorkLog.setLocationType(
        hasHoApplication ? WorkLocationType.HOME_OFFICE : WorkLocationType.OFFICE);

    timeData.saveWorkLog(newWorkLog);

    request.setStatus(WorkLogRequestStatus.APPROVED);
    request.setManager(reviewer);
    request.setManagerComment(resolveDto != null ? resolveDto.managerComment() : null);
    request.setResolvedAt(LocalDateTime.now());

    timeData.saveWorkLogRequest(request);
  }

  @Transactional
  public void rejectRequest(Long requestId, Integer reviewerId, WorkLogResolveRequest resolveDto) {
    WorkLogRequest request = getPendingRequest(requestId);
    User reviewer = getUserAndValidateAccess(reviewerId, request);

    request.setStatus(WorkLogRequestStatus.REJECTED);
    request.setManager(reviewer);
    request.setManagerComment(resolveDto != null ? resolveDto.managerComment() : null);
    request.setResolvedAt(LocalDateTime.now());

    timeData.saveWorkLogRequest(request);
  }

  @Transactional(readOnly = true)
  public List<WorkLogRequestDto> getEmployeeRequests(Integer employeeId) {
    return timeData.findWorkLogRequestsByEmployee(employeeId).stream().map(mapper::toDto).toList();
  }

  @Transactional(readOnly = true)
  public List<WorkLogRequestDto> getPendingRequestsForReviewer(Integer reviewerId) {
    User reviewer = userRepository.findById(reviewerId)
        .orElseThrow(() -> new UserException.NotFound(reviewerId));

    boolean isAdmin = reviewer.getRoles().stream()
        .anyMatch(role -> role.getName().equals("ROLE_ADMIN"));

    if (isAdmin) {
      return timeData.findAllPendingWorkLogRequests().stream().map(mapper::toDto).toList();
    } else {
      return timeData.findPendingWorkLogRequestsForManager(reviewerId).stream().map(mapper::toDto)
          .toList();
    }
  }

  private WorkLogRequest getPendingRequest(Long requestId) {
    WorkLogRequest request = timeData.findWorkLogRequestById(requestId)
        .orElseThrow(() -> new WorkLogRequestException.NotFound(requestId));

    if (request.getStatus() != WorkLogRequestStatus.PENDING) {
      throw new WorkLogRequestException.InvalidState();
    }
    return request;
  }

  private User getUserAndValidateAccess(Integer reviewerId, WorkLogRequest request) {
    User reviewer = userRepository.findById(reviewerId)
        .orElseThrow(() -> new UserException.NotFound(reviewerId));

    boolean isAdmin = reviewer.getRoles().stream()
        .anyMatch(role -> role.getName().equals("ROLE_ADMIN"));

    if (!isAdmin) {
      User employee = request.getEmployee();
      if (employee.getManager() == null || !employee.getManager().getUserId().equals(reviewerId)) {
        throw new WorkLogRequestException.UnauthorizedManager();
      }
    }
    return reviewer;
  }
}
