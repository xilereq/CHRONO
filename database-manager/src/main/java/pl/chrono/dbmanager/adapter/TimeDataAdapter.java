package pl.chrono.dbmanager.adapter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import pl.chrono.dbmanager.model.LeaveLog;
import pl.chrono.dbmanager.model.WorkLog;
import pl.chrono.dbmanager.model.WorkLogRequest;
import pl.chrono.dbmanager.model.enums.WorkLogRequestStatus;
import pl.chrono.dbmanager.ports.I_TimeData;
import pl.chrono.dbmanager.repository.LeaveLogRepository;
import pl.chrono.dbmanager.repository.WorkLogRepository;
import pl.chrono.dbmanager.repository.WorkLogRequestRepository;

@Component
@RequiredArgsConstructor
public class TimeDataAdapter implements I_TimeData {
  private final WorkLogRepository workLogRepository;
  private final LeaveLogRepository leaveLogRepository;
  private final WorkLogRequestRepository requestRepository;

  @Override
  public boolean isUserWorking(Integer userId) {
    return workLogRepository.existsByUser_UserIdAndEndTimeIsNull(userId);
  }

  @Override
  public WorkLog saveWorkLog(WorkLog workLog) {
    return workLogRepository.save(workLog);
  }

  @Override
  public Optional<WorkLog> findActiveWorkLog(Integer userId) {
    return workLogRepository.findByUser_UserIdAndEndTimeIsNull(userId);
  }

  @Override
  public List<WorkLog> findActiveWorkLogsBefore(LocalDateTime threshold) {
    return workLogRepository.findByEndTimeIsNullAndStartTimeBefore(threshold);
  }

  @Override
  public List<WorkLog> findUserWorkLogsAfter(Integer userId, LocalDateTime threshold) {
    return workLogRepository.findByUser_UserIdAndStartTimeAfter(userId, threshold);
  }

  @Override
  public boolean isUserOnLeave(Integer userId) {
    return leaveLogRepository.existsByUser_UserIdAndEndTimeIsNull(userId);
  }

  @Override
  public LeaveLog saveLeaveLog(LeaveLog leaveLog) {
    return leaveLogRepository.save(leaveLog);
  }

  @Override
  public Optional<LeaveLog> findActiveLeaveLog(Integer userId) {
    return leaveLogRepository.findByUser_UserIdAndEndTimeIsNull(userId);
  }

  @Override
  public List<WorkLog> findUnsettledWorkLogs() {
    return workLogRepository.findByEndTimeIsNotNullAndSettledFalse();
  }

  @Override
  public Optional<WorkLog> findLastClosedWorkLog(Integer userId) {
    return workLogRepository.findTopByUser_UserIdAndEndTimeIsNotNullOrderByEndTimeDesc(userId);
  }

  @Override
  public WorkLogRequest saveWorkLogRequest(WorkLogRequest request) {
    return requestRepository.save(request);
  }

  @Override
  public Optional<WorkLogRequest> findWorkLogRequestById(Long requestId) {
    return requestRepository.findById(requestId);
  }

  @Override
  public List<WorkLogRequest> findWorkLogRequestsByEmployee(Integer employeeId) {
    return requestRepository.findAllByEmployeeUserIdOrderByCreatedAtDesc(employeeId);
  }

  @Override
  public List<WorkLogRequest> findPendingWorkLogRequestsForManager(Integer managerId) {
    return requestRepository.findAllByEmployeeManagerUserIdAndStatus(
        managerId, WorkLogRequestStatus.PENDING);
  }

  @Override
  public List<WorkLog> findWorkLogsByUserAndDate(Integer userId, LocalDateTime startOfDay,
                                                 LocalDateTime endOfDay) {
    return workLogRepository.findByUser_UserIdAndStartTimeBetweenOrderByStartTimeAsc(
        userId, startOfDay, endOfDay);
  }

  @Override
  public List<WorkLogRequest> findAllPendingWorkLogRequests() {
    return requestRepository.findAllByStatus(WorkLogRequestStatus.PENDING);
  }

  @Override
  public void deleteWorkLog(WorkLog workLog) {
    workLogRepository.delete(workLog);
  }
}
