package pl.chrono.timemanager.service;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.chrono.dbmanager.model.OperationLog;
import pl.chrono.dbmanager.model.User;
import pl.chrono.dbmanager.model.WorkLog;
import pl.chrono.dbmanager.ports.I_TimeData;
import pl.chrono.dbmanager.repository.OperationLogRepository;
import pl.chrono.dbmanager.repository.UserRepository;

@Service
@RequiredArgsConstructor
public class ScheduleService {
  private final I_TimeData timeData;
  private final UserRepository userRepository;
  private final OperationLogRepository logRepository;

  @Value("${chrono.max-work-hours:12}")
  private int maxWorkHours;

  @Transactional
  public void closeTooLongWorkLogs() {
    LocalDateTime threshold = LocalDateTime.now().minusHours(maxWorkHours);
    timeData.findActiveWorkLogsBefore(threshold).forEach(workLog -> {
      LocalDateTime now = LocalDateTime.now();
      long workTime = workLog.calculateWorkTime(now);
      if (workTime <= maxWorkHours * 3600L) {
        return;
      }

      LocalDateTime closeTime = workLog.getStartTime()
          .plusHours(maxWorkHours)
          .plusSeconds(workLog.calculateBreakTime(now));
      workLog.getLeaveLogs().stream()
          .filter(leaveLog -> leaveLog.getEndTime() == null
              || leaveLog.getEndTime().isAfter(closeTime))
          .forEach(leaveLog -> {
            leaveLog.setEndTime(closeTime);
            timeData.saveLeaveLog(leaveLog);
          });
      workLog.setEndTime(closeTime);
      timeData.saveWorkLog(workLog);

      OperationLog auditLog = new OperationLog();
      auditLog.setAction("SYSTEM_CLOSE_WORKLOG");
      auditLog.setDetails("System has automatically closed a work log for user "
          + workLog.getUser().getName() + " " + workLog.getUser().getSurname() + ".");
      auditLog.setUserEmail("SYSTEM");
      auditLog.setTimestamp(LocalDateTime.now());
      logRepository.save(auditLog);
    });
  }

  @Transactional
  public void countOvertime() {
    List<WorkLog> unsettledWorkLogs = timeData.findUnsettledWorkLogs();
    if (unsettledWorkLogs.isEmpty()) {
      return;
    }
    Set<User> usersToUpdate = new HashSet<>();
    int limit = 8 * 3600;
    int processedLogsCount = 0;

    for (WorkLog workLog : unsettledWorkLogs) {
      if (workLog.getEndTime() != null) {
        long workTime = workLog.calculateWorkTime(workLog.getEndTime());
        int overtimeSeconds = (int) Math.max(0, workTime - limit);
        workLog.setSettled(true);
        timeData.saveWorkLog(workLog);

        User user = workLog.getUser();
        int currentBalance = user.getOvertimeBalance() != null ? user.getOvertimeBalance() : 0;
        user.setOvertimeBalance(currentBalance + overtimeSeconds);
        usersToUpdate.add(user);
        processedLogsCount++;
      }
    }
    userRepository.saveAll(usersToUpdate);

    if (processedLogsCount > 0) {
      OperationLog auditLog = new OperationLog();
      auditLog.setAction("SYSTEM_COUNT_OVERTIME");
      auditLog.setDetails(String.format(
          "Overtime hours have been calculated. %d work logs have been updated for %d workers.",
          processedLogsCount, usersToUpdate.size()
      ));
      auditLog.setUserEmail("SYSTEM");
      auditLog.setTimestamp(LocalDateTime.now());
      logRepository.save(auditLog);
    }
  }
}
