package pl.chrono.dbmanager.ports;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import pl.chrono.dbmanager.model.LeaveLog;
import pl.chrono.dbmanager.model.WorkLog;
import pl.chrono.dbmanager.model.WorkLogRequest;

public interface I_TimeData {
  boolean isUserWorking(Integer userId);

  WorkLog saveWorkLog(WorkLog workLog);

  Optional<WorkLog> findActiveWorkLog(Integer userId);

  List<WorkLog> findActiveWorkLogsBefore(LocalDateTime threshold);

  List<WorkLog> findUserWorkLogsAfter(Integer userId, LocalDateTime threshold);

  boolean isUserOnLeave(Integer userId);

  LeaveLog saveLeaveLog(LeaveLog leaveLog);

  Optional<LeaveLog> findActiveLeaveLog(Integer userId);

  List<WorkLog> findUnsettledWorkLogs();

  List<WorkLog> findPendingCorrectionBefore(LocalDateTime threshold);

  Optional<WorkLog> findLastClosedWorkLog(Integer userId);

  WorkLogRequest saveWorkLogRequest(WorkLogRequest request);

  Optional<WorkLogRequest> findWorkLogRequestById(Long requestId);

  List<WorkLogRequest> findWorkLogRequestsByEmployee(Integer employeeId);

  List<WorkLogRequest> findPendingWorkLogRequestsForManager(Integer managerId);

  List<WorkLog> findWorkLogsByUserAndDate(Integer userId, LocalDateTime startOfDay,
                                          LocalDateTime endOfDay);

  List<WorkLogRequest> findAllPendingWorkLogRequests();

  void deleteWorkLog(WorkLog workLog);
}
