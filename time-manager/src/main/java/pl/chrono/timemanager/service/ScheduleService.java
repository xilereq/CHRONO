package pl.chrono.timemanager.service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.chrono.dbmanager.model.Application;
import pl.chrono.dbmanager.model.OperationLog;
import pl.chrono.dbmanager.model.Policy;
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
  private final WorkTimeCalculationService workTimeCalculationService;

  @Transactional
  public void processWorkLogs() {
    LocalDateTime now = LocalDateTime.now();
    int autoClosedCount = 0;
    int settledCount = 0;

    List<WorkLog> activeWorkLogs = timeData.findActiveWorkLogsBefore(now);

    for (WorkLog workLog : activeWorkLogs) {
      LocalDate workDate = workLog.getStartTime() != null ? workLog.getStartTime()
          .toLocalDate() : now.toLocalDate();
      Policy policyForDay = workLog.getUser().getPolicyForDate(workDate);
      int maxAllowedSeconds = workTimeCalculationService.getMaxAllowedSeconds(policyForDay);
      long activeSeconds = workLog.calculateWorkTime(now);

      boolean hasForgottenToCloseBreak = workLog.getLeaveLogs().stream()
          .anyMatch(l -> l.getEndTime() == null &&
              Duration.between(l.getStartTime(), now).toHours() > 4);

      if (activeSeconds <= maxAllowedSeconds && !hasForgottenToCloseBreak) {
        continue;
      }

      int targetSeconds = workTimeCalculationService.getAdjustedRequiredSeconds(
          workLog.getUser().getUserId(), workDate, policyForDay);

      workLog.forceClose(now, targetSeconds);
      workLog.setSettled(false);
      timeData.saveWorkLog(workLog);
      autoClosedCount++;
      eventPublisher.publishEvent(new LawViolationEvent(
          workLog.getUser().getUserId(), "AUTO_CLOSE", workLog.getWorkLogId().longValue(),
          "WORK_LOG",
          "Your last work log has been automatically closed after exceeding the maximum allowed " +
              "duration."
      ));
    }
    List<WorkLog> unsettledWorkLogs = timeData.findUnsettledWorkLogs();

    for (WorkLog workLog : unsettledWorkLogs) {
      workLog.setSettled(true);
      timeData.saveWorkLog(workLog);
      settledCount++;

      Integer userId = workLog.getUser().getUserId();
      LocalDate workDate = workLog.getStartTime().toLocalDate();
      Policy policyForDay = workLog.getUser().getPolicyForDate(workDate);
      int baseRequiredSeconds = workTimeCalculationService.getBaseRequiredSeconds(
          workDate, policyForDay);
      int roundingSeconds = workTimeCalculationService.getRoundingSeconds(policyForDay);
      long workTime = workLog.getEffectiveWorkTime(now, baseRequiredSeconds, roundingSeconds);

      List<Application> dailyApps = applicationRepository.findApprovedByEmployeeAndDate(
          userId, workDate);

      if (workTime > baseRequiredSeconds) {
        dailyApps.stream().filter(app -> app.getType() == ApplicationType.OVERTIME_EARN).findFirst()
            .ifPresent(app -> overtimeService.grantOvertime(app, workLog));

      } else if (workTime < baseRequiredSeconds) {
        long shortageSeconds = baseRequiredSeconds - workTime;
        List<Application> coveringApps = dailyApps.stream().filter(
                app -> app.getType() == ApplicationType.EARLY_EXIT ||
                    app.getType() == ApplicationType.OVERTIME_TAKE).toList();
        for (Application app : coveringApps) {
          if (app.getType() == ApplicationType.EARLY_EXIT) {
            overtimeService.registerDebt(app, workLog);
          } else {
            overtimeService.changeOvertimeStatus(app, workLog);
          }
        }
        long coveredSeconds = coveringApps.stream()
            .mapToLong(app -> app.getDurationSeconds() != null ? app.getDurationSeconds() : 0)
            .sum();
        long unexcusedShortage = shortageSeconds - coveredSeconds;
        if (unexcusedShortage > 0) {
          overtimeService.registerPenaltyDebt(workLog, (int) unexcusedShortage);
          eventPublisher.publishEvent(new LawViolationEvent(
              userId, "VIOLATION_UNEXCUSED_ABSENCE", workLog.getWorkLogId().longValue(), "WORK_LOG",
              "WARNING: You worked less than required. An unexcused shortage of " +
                  workTimeCalculationService.formatSecondsToTime(unexcusedShortage) + " has been added as a penalty debt."
          ));
        }
      }
    }

    if (settledCount > 0 || autoClosedCount > 0) {
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
    List<Integer> userIds = userRepository.findAllUserIds();
    int processedCount = 0;

    for (Integer userId : userIds) {
      overtimeService.expireUnusedOvertime(userId);
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
