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
import pl.chrono.dbmanager.model.Policy;
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
    int flaggedCount = flagUnclosedWorkLogs(now);
    int abandonedCount = removeAbandonedWorkLogs(now);
    int settledCount = settleClosedWorkLogs(now);

    if (flaggedCount > 0 || abandonedCount > 0 || settledCount > 0) {
      saveAuditLog(settledCount, flaggedCount, abandonedCount);
    }
  }

  int flagUnclosedWorkLogs(LocalDateTime now) {
    int flaggedCount = 0;
    LocalDateTime startOfToday = now.toLocalDate().atStartOfDay();
    List<WorkLog> activeWorkLogs = timeData.findActiveWorkLogsBefore(startOfToday);

    for (WorkLog workLog : activeWorkLogs) {
      LocalDate workDate = workLog.getStartTime() != null
          ? workLog.getStartTime().toLocalDate() : now.toLocalDate();
      workLog.getLeaveLogs().stream()
          .filter(leave -> leave.getEndTime() == null)
          .forEach(leave -> leave.setEndTime(now));

      workLog.setPendingCorrection(true);
      timeData.saveWorkLog(workLog);
      flaggedCount++;

      eventPublisher.publishEvent(new LawViolationEvent(
          workLog.getUser().getUserId(), "UNCLOSED_WORK_LOG",
          workLog.getWorkLogId().longValue(), "WORK_LOG",
          "UWAGA: Posiadasz niezamknięty log pracy w dniu " + workDate +
              ". Proszę przesłać wniosek o korekcję czasu pracy w przeciągu 2 tygodni."
      ));
    }

    return flaggedCount;
  }

  int removeAbandonedWorkLogs(LocalDateTime now) {
    int abandonedCount = 0;
    List<WorkLog> abandonedWorkLogs = timeData.findPendingCorrectionBefore(
        now.toLocalDate().minusWeeks(2).atStartOfDay());

    for (WorkLog workLog : abandonedWorkLogs) {
      LocalDate workDate = workLog.getStartTime() != null
          ? workLog.getStartTime().toLocalDate() : now.toLocalDate();
      Policy policyForDay = workLog.getUser().getPolicyForDate(workDate);
      int missingSeconds = workTimeCalculationService.getBaseRequiredSeconds(workDate, policyForDay);

      if (missingSeconds > 0) {
        overtimeService.registerPenaltyDebt(workLog.getUser(), workDate, missingSeconds);
        eventPublisher.publishEvent(new LawViolationEvent(
            workLog.getUser().getUserId(), "ABANDONED_WORK_LOG",
            workLog.getWorkLogId().longValue(), "WORK_LOG",
            "Twój log z dnia " + workDate + " został usunięty przez brak przesłania korekty " +
                "w ciągu 2 tygodni. Została naliczona kara za dzień."
        ));
      }

      timeData.deleteWorkLog(workLog);
      abandonedCount++;
    }

    return abandonedCount;
  }

  int settleClosedWorkLogs(LocalDateTime now) {
    int settledCount = 0;
    List<WorkLog> unsettledWorkLogs = timeData.findUnsettledWorkLogs();

    for (WorkLog workLog : unsettledWorkLogs) {
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
        dailyApps.stream()
            .filter(app -> app.getType() == ApplicationType.OVERTIME_EARN)
            .findFirst()
            .ifPresent(app -> overtimeService.grantOvertime(app, workLog));

      } else if (workTime < baseRequiredSeconds) {
        settleShortage(workLog, userId, baseRequiredSeconds, workTime, dailyApps);
      }

      workLog.setSettled(true);
      timeData.saveWorkLog(workLog);
      settledCount++;
    }

    return settledCount;
  }

  private void settleShortage(WorkLog workLog, Integer userId, int baseRequiredSeconds,
                              long workTime, List<Application> dailyApps) {
    long shortageSeconds = baseRequiredSeconds - workTime;
    List<Application> coveringApps = dailyApps.stream()
        .filter(app -> app.getType() == ApplicationType.EARLY_EXIT ||
            app.getType() == ApplicationType.OVERTIME_TAKE)
        .toList();

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
              workTimeCalculationService.formatSecondsToTime(unexcusedShortage) +
              " has been added as a penalty debt."
      ));
    }
  }

  void saveAuditLog(int settledCount, int flaggedCount, int abandonedCount) {
    OperationLog auditLog = new OperationLog();
    auditLog.setAction("SYSTEM_PROCESS_WORKLOGS");
    auditLog.setDetails(String.format(
        "System processed %d work log(s): %d settled, %d flagged for correction, %d abandoned and removed.",
        settledCount + flaggedCount + abandonedCount, settledCount, flaggedCount, abandonedCount
    ));
    auditLog.setUserEmail("SYSTEM");
    auditLog.setTimestamp(LocalDateTime.now());
    logRepository.save(auditLog);
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
