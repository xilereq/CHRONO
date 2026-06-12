package pl.chrono.dbmanager.ports;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import pl.chrono.dbmanager.model.LeaveLog;
import pl.chrono.dbmanager.model.WorkLog;

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

  Optional<WorkLog> findLastClosedWorkLog(Integer userId);
}
