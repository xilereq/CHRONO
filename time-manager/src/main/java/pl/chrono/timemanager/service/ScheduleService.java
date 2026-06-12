package pl.chrono.timemanager.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.chrono.dbmanager.model.Application;
import pl.chrono.dbmanager.model.OperationLog;
import pl.chrono.dbmanager.model.User;
import pl.chrono.dbmanager.model.WorkLog;
import pl.chrono.dbmanager.model.enums.ApplicationType;
import pl.chrono.dbmanager.ports.I_TimeData;
import pl.chrono.dbmanager.repository.ApplicationRepository;
import pl.chrono.dbmanager.repository.OperationLogRepository;
import pl.chrono.dbmanager.repository.UserRepository;
import pl.chrono.timemanager.event.LawViolationEvent;


@Service
@RequiredArgsConstructor
public class ScheduleService {
  private final I_TimeData timeData;
  private final OperationLogRepository logRepository;
  private final OvertimeService overtimeService;
  private final ApplicationRepository applicationRepository;
  private final ApplicationEventPublisher eventPublisher;
  private final UserRepository userRepository;

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
        eventPublisher.publishEvent(new LawViolationEvent(
            workLog.getUser().getUserId(), "AUTO_CLOSE", workLog.getWorkLogId().longValue(),
            "WORK_LOG",
            "Your last work log has been automatically closed after " + "exceeding the maximum " + "allowed duration."
        ));
      }

      workLog.setSettled(true);
      timeData.saveWorkLog(workLog);
      settledCount++;

      Integer userId = workLog.getUser().getUserId();
      LocalDate workDate = workLog.getStartTime().toLocalDate();
      long workTime = workLog.getEffectiveWorkTime(now);
      int requiredSeconds = workLog.getRequiredSeconds();

      List<Application> dailyApps = applicationRepository.findApprovedByEmployeeAndDate(
          userId, workDate);

      if (workTime > requiredSeconds) {
        dailyApps.stream().filter(app -> app.getType() == ApplicationType.OVERTIME_EARN).findFirst()
            .ifPresent(app -> overtimeService.grantOvertime(app, workLog));

      } else if (workTime < requiredSeconds) {
        dailyApps.stream().filter(
                app -> app.getType() == ApplicationType.EARLY_EXIT || app.getType() == ApplicationType.OVERTIME_TAKE)
            .forEach(app -> {
              if (app.getType() == ApplicationType.EARLY_EXIT) {
                overtimeService.registerDebt(app, workLog);
              } else {
                overtimeService.changeOvertimeStatus(app, workLog);
              }
            });
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

  @Transactional
  public void processOvertimeExpirations() {
    List<User> users = userRepository.findAll();
    int processedCount = 0;

    for (User user : users) {
      overtimeService.expireUnusedOvertime(user.getUserId());
      processedCount++;
    }

    OperationLog auditLog = new OperationLog();
    auditLog.setAction("SYSTEM_EXPIRE_OVERTIME");
    auditLog.setDetails(
        "System verified and expired unused overtime for " + processedCount + " users.");
    auditLog.setUserEmail("SYSTEM");
    auditLog.setTimestamp(LocalDateTime.now());
    logRepository.save(auditLog);
  }
}
