package pl.chrono.timemanager.service;

import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.chrono.dbmanager.model.OperationLog;
import pl.chrono.dbmanager.model.WorkLog;
import pl.chrono.dbmanager.ports.I_TimeData;
import pl.chrono.dbmanager.repository.OperationLogRepository;

@Service
@RequiredArgsConstructor
public class ScheduleService {
  private final I_TimeData timeData;
  private final OperationLogRepository logRepository;
  private final OvertimeService overtimeService;

  @Transactional
  public void processWorkLogs() {
    List<WorkLog> unsettledWorkLogs = timeData.findUnsettledWorkLogs();
    if (unsettledWorkLogs.isEmpty()) {
      return;
    }
    int autoClosedCount = 0;
    int settledCount = 0;
    LocalDateTime now = LocalDateTime.now();

    for (WorkLog workLog : unsettledWorkLogs) {
      if (workLog.getEndTime() == null) {
        long activeSeconds = workLog.calculateWorkTime(now);
        if (activeSeconds <= workLog.getMaxAllowedSeconds()) {
          continue;
        }
        workLog.forceClose(now);
        autoClosedCount++;
      }

      long workTime = workLog.getEffectiveWorkTime(workLog.getEndTime());
      int requiredSeconds = workLog.getRequiredSeconds();
      int overtimeSeconds = (int) Math.max(0, workTime - requiredSeconds);
      workLog.setSettled(true);
      timeData.saveWorkLog(workLog);
      settledCount++;

      if (overtimeSeconds >= workLog.getOvertimeThresholdSeconds()) {
        overtimeService.grantOvertime(overtimeSeconds, workLog);
      }
    }
    if (settledCount > 0) {
      OperationLog auditLog = new OperationLog();
      auditLog.setAction("SYSTEM_PROCESS_WORKLOGS");
      auditLog.setDetails(String.format(
          "System has automatically processed %d work log(s)," + "including closing %d of them.",
          settledCount, autoClosedCount
      ));
      auditLog.setUserEmail("SYSTEM");
      auditLog.setTimestamp(LocalDateTime.now());
      logRepository.save(auditLog);
    }
  }
}
