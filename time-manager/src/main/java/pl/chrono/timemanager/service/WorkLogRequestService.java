package pl.chrono.timemanager.service;

import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.chrono.dbmanager.model.Policy;
import pl.chrono.dbmanager.model.User;
import pl.chrono.dbmanager.model.WorkLog;
import pl.chrono.dbmanager.model.WorkLogRequest;
import pl.chrono.dbmanager.model.enums.WorkLogRequestStatus;
import pl.chrono.dbmanager.model.enums.WorkLogRequestType;
import pl.chrono.dbmanager.ports.I_TimeData;
import pl.chrono.dbmanager.repository.UserRepository;
import pl.chrono.timemanager.dto.request.WorkLogCreateRequest;
import pl.chrono.timemanager.dto.request.WorkLogResolveRequest;
import pl.chrono.timemanager.dto.response.WorkLogRequestDto;
import pl.chrono.timemanager.mapper.WorkLogRequestMapper;

@Service
@RequiredArgsConstructor
public class WorkLogRequestService {

  private final UserRepository userRepository;
  private final I_TimeData timeData;
  private final WorkLogRequestMapper mapper;

  @Transactional
  public void submitRequest(Integer employeeId, WorkLogCreateRequest requestDto) {
    LocalDateTime proposedStart = requestDto.proposedStartTime();
    LocalDateTime proposedEnd = requestDto.proposedEndTime();

    if (proposedStart.isAfter(proposedEnd) || proposedStart.isEqual(proposedEnd)) {
      throw new IllegalArgumentException("Proposed start time must be before end time.");
    }

    User employee = userRepository.findById(employeeId)
        .orElseThrow(() -> new IllegalArgumentException("User not found"));

    Policy policy = employee.getPolicyForDate(proposedStart.toLocalDate());
    if (policy != null) {
      long requestedSeconds = java.time.Duration.between(proposedStart, proposedEnd).getSeconds();
      if (requestedSeconds > policy.getMaxAllowedSeconds()) {
        throw new IllegalArgumentException(
            "Requested work time exceeds maximum allowed daily hours (" + policy.getMaxDailyHours() + "h).");
      }
    }

    WorkLogRequest request = new WorkLogRequest();
    request.setEmployee(employee);
    request.setType(requestDto.type());
    request.setProposedStartTime(proposedStart);
    request.setProposedEndTime(proposedEnd);
    request.setReason(requestDto.reason());
    request.setStatus(WorkLogRequestStatus.PENDING);

    if (requestDto.type() == WorkLogRequestType.UPDATE) {
      if (requestDto.targetWorkLogId() == null) {
        throw new IllegalArgumentException("Target WorkLog ID is required for UPDATE requests.");
      }

      WorkLog targetLog = timeData.findWorkLogById(requestDto.targetWorkLogId())
          .orElseThrow(() -> new IllegalArgumentException("Target WorkLog not found"));

      if (!targetLog.getUser().getUserId().equals(employeeId)) {
        throw new IllegalArgumentException("You cannot request changes to another user's WorkLog.");
      }

      request.setTargetWorkLog(targetLog);
    }

    timeData.saveWorkLogRequest(request);
  }

  @Transactional
  public void approveRequest(Long requestId, Integer managerId, WorkLogResolveRequest resolveDto) {
    WorkLogRequest request = getPendingRequest(requestId);
    validateDirectManager(request, managerId);
    executeApproval(request, managerId, resolveDto);
  }

  @Transactional
  public void rejectRequest(Long requestId, Integer managerId, WorkLogResolveRequest resolveDto) {
    WorkLogRequest request = getPendingRequest(requestId);
    validateDirectManager(request, managerId);
    executeRejection(request, managerId, resolveDto);
  }

  @Transactional
  public void adminApproveRequest(Long requestId, Integer adminId,
                                  WorkLogResolveRequest resolveDto) {
    WorkLogRequest request = getPendingRequest(requestId);
    executeApproval(request, adminId, resolveDto);
  }

  @Transactional
  public void adminRejectRequest(Long requestId, Integer adminId,
                                 WorkLogResolveRequest resolveDto) {
    WorkLogRequest request = getPendingRequest(requestId);
    executeRejection(request, adminId, resolveDto);
  }

  @Transactional(readOnly = true)
  public List<WorkLogRequestDto> getEmployeeRequests(Integer employeeId) {
    return timeData.findWorkLogRequestsByEmployee(employeeId).stream().map(mapper::toDto).toList();
  }

  @Transactional(readOnly = true)
  public List<WorkLogRequestDto> getPendingRequestsForManager(Integer managerId) {
    return timeData.findPendingWorkLogRequestsForManager(managerId).stream().map(mapper::toDto)
        .toList();
  }

  @Transactional(readOnly = true)
  public List<WorkLogRequestDto> getAllPendingRequests() {
    return timeData.findAllPendingWorkLogRequests()
        .stream().map(mapper::toDto).toList();
  }

  private void validateDirectManager(WorkLogRequest request, Integer managerId) {
    User employee = request.getEmployee();
    if (employee.getManager() == null || !employee.getManager().getUserId().equals(managerId)) {
      throw new SecurityException(
          "Brak uprawnień. Tylko bezpośredni przełożony może rozpatrywać wnioski tego pracownika.");
    }
  }

  private WorkLogRequest getPendingRequest(Long requestId) {
    WorkLogRequest request = timeData.findWorkLogRequestById(requestId)
        .orElseThrow(() -> new IllegalArgumentException("Request not found"));

    if (request.getStatus() != WorkLogRequestStatus.PENDING) {
      throw new IllegalStateException("Only PENDING requests can be processed.");
    }
    return request;
  }

  private void executeApproval(WorkLogRequest request, Integer reviewerId,
                               WorkLogResolveRequest resolveDto) {
    User reviewer = userRepository.findById(reviewerId)
        .orElseThrow(() -> new IllegalArgumentException("Reviewer not found"));

    if (request.getType() == WorkLogRequestType.CREATE) {
      WorkLog newWorkLog = new WorkLog();
      newWorkLog.setUser(request.getEmployee());
      newWorkLog.setStartTime(request.getProposedStartTime());
      newWorkLog.setEndTime(request.getProposedEndTime());
      timeData.saveWorkLog(newWorkLog);

    } else if (request.getType() == WorkLogRequestType.UPDATE) {
      WorkLog targetLog = request.getTargetWorkLog();
      targetLog.setStartTime(request.getProposedStartTime());
      targetLog.setEndTime(request.getProposedEndTime());

      if (targetLog.getLeaveLogs() != null) {
        targetLog.getLeaveLogs().clear();
      }
      timeData.saveWorkLog(targetLog);
    }

    request.setStatus(WorkLogRequestStatus.APPROVED);
    request.setManager(reviewer);
    request.setManagerComment(resolveDto != null ? resolveDto.managerComment() : null);
    request.setResolvedAt(LocalDateTime.now());

    timeData.saveWorkLogRequest(request);
  }

  private void executeRejection(WorkLogRequest request, Integer reviewerId,
                                WorkLogResolveRequest resolveDto) {
    User reviewer = userRepository.findById(reviewerId)
        .orElseThrow(() -> new IllegalArgumentException("Reviewer not found"));

    request.setStatus(WorkLogRequestStatus.REJECTED);
    request.setManager(reviewer);
    request.setManagerComment(resolveDto != null ? resolveDto.managerComment() : null);
    request.setResolvedAt(LocalDateTime.now());

    timeData.saveWorkLogRequest(request);
  }
}
