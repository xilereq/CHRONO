package pl.chrono.timemanager.service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.chrono.dbmanager.model.Policy;
import pl.chrono.dbmanager.model.User;
import pl.chrono.dbmanager.model.WorkLog;
import pl.chrono.dbmanager.model.WorkLogRequest;
import pl.chrono.dbmanager.model.enums.WorkLocationType;
import pl.chrono.dbmanager.model.enums.WorkLogRequestStatus;
import pl.chrono.dbmanager.ports.I_TimeData;
import pl.chrono.dbmanager.ports.I_UserData;
import pl.chrono.timemanager.api.I_WorkLogRequestService;
import pl.chrono.timemanager.dto.request.WorkLogCreateRequest;
import pl.chrono.timemanager.dto.request.WorkLogResolveRequest;
import pl.chrono.timemanager.dto.response.WorkLogRequestDto;
import pl.chrono.timemanager.event.WorkLogRequestNotificationEvent;
import pl.chrono.timemanager.exception.UserException;
import pl.chrono.timemanager.exception.WorkLogRequestException;
import pl.chrono.timemanager.mapper.WorkLogRequestMapper;
import pl.chrono.usermanager.api.I_ManagerDelegationService;

@Service
@RequiredArgsConstructor
public class WorkLogRequestService implements I_WorkLogRequestService {

  private final I_UserData userData;
  private final I_TimeData timeData;
  private final OvertimeService overtimeService;
  private final WorkLogRequestMapper mapper;
  private final WorkTimeCalculationService workTimeCalculationService;
  private final ApplicationEventPublisher eventPublisher;
  private final I_ManagerDelegationService delegationService;

  @Transactional
  @Override
  public void submitRequest(Integer employeeId, WorkLogCreateRequest requestDto) {
    LocalDateTime proposedStart = requestDto.proposedStartTime();
    LocalDateTime proposedEnd = requestDto.proposedEndTime();

    if (proposedStart.isAfter(proposedEnd) || proposedStart.isEqual(proposedEnd)) {
      throw new WorkLogRequestException.InvalidTimeRange();
    }

    User employee = userData.findById(employeeId)
        .orElseThrow(() -> new UserException.NotFound(employeeId));

    LocalDate targetDate = proposedStart.toLocalDate();
    workTimeCalculationService.validateEmploymentPeriod(employee, targetDate);
    Policy policy = employee.getPolicyForDate(targetDate);
    long requestedSeconds = Duration.between(proposedStart, proposedEnd).getSeconds();

    if (policy != null && requestedSeconds > policy.getMaxAllowedSeconds()) {
      throw new WorkLogRequestException.DailyLimitExceeded(policy.getMaxDailyHours());
    }

    int requiredSeconds = workTimeCalculationService.getAdjustedRequiredSeconds(
        employeeId, targetDate, policy);
    if (requestedSeconds < requiredSeconds) {
      throw new WorkLogRequestException.InsufficientTime(
          workTimeCalculationService.formatSecondsToTime(requiredSeconds));
    }

    WorkLogRequest request = new WorkLogRequest();
    request.setEmployee(employee);
    request.setProposedStartTime(proposedStart);
    request.setProposedEndTime(proposedEnd);
    request.setReason(requestDto.reason());
    request.setStatus(WorkLogRequestStatus.PENDING);

    timeData.saveWorkLogRequest(request);
    publishNotificationEvent(request, "CREATED", null);
  }

  @Transactional
  @Override
  public void approveRequest(Long requestId, Integer reviewerId, WorkLogResolveRequest resolveDto) {
    WorkLogRequest request = getPendingRequest(requestId);
    User reviewer = getUserAndValidateAccess(reviewerId, request);

    LocalDate targetDate = request.getProposedStartTime().toLocalDate();
    workTimeCalculationService.validateEmploymentPeriod(request.getEmployee(), targetDate);
    LocalDateTime startOfDay = targetDate.atStartOfDay();
    LocalDateTime endOfDay = targetDate.plusDays(1).atStartOfDay().minusNanos(1);

    List<WorkLog> existingLogs = timeData.findWorkLogsByUserAndDate(
        request.getEmployee().getUserId(), startOfDay, endOfDay);

    boolean hasHoApplication = workTimeCalculationService.hasApprovedHomeOffice(
        request.getEmployee().getUserId(), targetDate);
    WorkLocationType location = hasHoApplication ? WorkLocationType.HOME_OFFICE :
        WorkLocationType.OFFICE;

    if (existingLogs.isEmpty()) {
      WorkLog newWorkLog = new WorkLog();
      newWorkLog.setUser(request.getEmployee());
      newWorkLog.setStartTime(request.getProposedStartTime());
      newWorkLog.setEndTime(request.getProposedEndTime());
      newWorkLog.setLocationType(location);
      timeData.saveWorkLog(newWorkLog);
    } else {
      WorkLog primaryLog = existingLogs.getFirst();
      primaryLog.setStartTime(request.getProposedStartTime());
      primaryLog.setEndTime(request.getProposedEndTime());
      primaryLog.setLocationType(location);

      if (primaryLog.getLeaveLogs() != null) {
        primaryLog.getLeaveLogs().clear();
      }

      primaryLog.setPendingCorrection(false);
      if (primaryLog.isSettled()) {
        overtimeService.revertSettlementForWorkLog(primaryLog.getWorkLogId());
        primaryLog.setSettled(false);
      }
      timeData.saveWorkLog(primaryLog);

      for (int i = 1; i < existingLogs.size(); i++) {
        timeData.deleteWorkLog(existingLogs.get(i));
      }
    }

    request.setStatus(WorkLogRequestStatus.APPROVED);
    request.setManager(reviewer);
    request.setManagerComment(resolveDto != null ? resolveDto.managerComment() : null);
    request.setResolvedAt(LocalDateTime.now());

    timeData.saveWorkLogRequest(request);
    publishNotificationEvent(request, "STATUS_CHANGED", reviewer);
  }

  @Transactional
  @Override
  public void rejectRequest(Long requestId, Integer reviewerId, WorkLogResolveRequest resolveDto) {
    WorkLogRequest request = getPendingRequest(requestId);
    User reviewer = getUserAndValidateAccess(reviewerId, request);

    request.setStatus(WorkLogRequestStatus.REJECTED);
    request.setManager(reviewer);
    request.setManagerComment(resolveDto != null ? resolveDto.managerComment() : null);
    request.setResolvedAt(LocalDateTime.now());

    timeData.saveWorkLogRequest(request);
    publishNotificationEvent(request, "STATUS_CHANGED", reviewer);
  }

  @Transactional(readOnly = true)
  @Override
  public List<WorkLogRequestDto> getEmployeeRequests(Integer employeeId) {
    return timeData.findWorkLogRequestsByEmployee(employeeId).stream().map(mapper::toDto).toList();
  }

  @Transactional(readOnly = true)
  @Override
  public List<WorkLogRequestDto> getPendingRequestsForReviewer(Integer reviewerId) {
    User reviewer = userData.findById(reviewerId)
        .orElseThrow(() -> new UserException.NotFound(reviewerId));

    boolean isAdmin = reviewer.getRoles().stream()
        .anyMatch(role -> role.getName().equals("ROLE_ADMIN"));

    if (isAdmin) {
      return timeData.findAllPendingWorkLogRequests().stream().map(mapper::toDto).toList();
    }

    Stream<WorkLogRequest> ownTeam = timeData.findPendingWorkLogRequestsForManager(reviewerId).stream();
    Stream<WorkLogRequest> substitutedTeams = delegationService.getRepresentedManagerIds(reviewerId).stream()
        .filter(managerId -> !managerId.equals(reviewerId))
        .flatMap(managerId -> timeData.findPendingWorkLogRequestsForManager(managerId).stream())
        .filter(request -> !reviewerId.equals(request.getEmployee().getUserId()));

    return Stream.concat(ownTeam, substitutedTeams).map(mapper::toDto).toList();
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
    User reviewer = userData.findById(reviewerId)
        .orElseThrow(() -> new UserException.NotFound(reviewerId));

    boolean isAdmin = reviewer.getRoles().stream()
        .anyMatch(role -> role.getName().equals("ROLE_ADMIN"));

    if (!isAdmin && !canActAsManagerOf(reviewerId, request.getEmployee())) {
      throw new WorkLogRequestException.UnauthorizedManager();
    }
    return reviewer;
  }

  private boolean canActAsManagerOf(Integer reviewerId, User employee) {
    if (employee.getManager() == null || reviewerId.equals(employee.getUserId())) {
      return false;
    }
    Integer managerId = employee.getManager().getUserId();
    return managerId.equals(reviewerId) || delegationService.isActiveDeputyOf(reviewerId, managerId);
  }

  void publishNotificationEvent(WorkLogRequest request, String action, User reviewer) {
    Integer managerId = request.getEmployee().getManager() != null ?
        request.getEmployee().getManager().getUserId() : null;

    String reviewerFullName = reviewer != null ?
        reviewer.getName() + " " + reviewer.getSurname() : null;

    eventPublisher.publishEvent(new WorkLogRequestNotificationEvent(
        request.getId(),
        request.getEmployee().getUserId(),
        managerId,
        action,
        request.getStatus().name(),
        reviewerFullName
    ));
  }
}
