package pl.chrono.timemanager.service;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import pl.chrono.dbmanager.repository.OperationLogRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class WorkTaskScheduler {
  private final ScheduleService scheduleService;
  private final OperationLogRepository operationLogRepository;

  @Scheduled(cron = "0 0 4 * * *")
  public void runCountOvertime() {
    scheduleService.processWorkLogs();
  }

  @Scheduled(cron = "0 30 4 1 1,4,7,10 *")
  public void runExpireOvertime() {
    scheduleService.processOvertimeExpirations();
  }

  @EventListener(ApplicationReadyEvent.class)
  public void runOnStartup() {
    scheduleService.processWorkLogs();

    runOvertimeExpireIfItFailedThisQuarter();
  }

  private void runOvertimeExpireIfItFailedThisQuarter() {
    LocalDate today = LocalDate.now();
    int currentMonth = today.getMonthValue();
    int currentYear = today.getYear();

    int firstMonthOfQuarter = ((currentMonth - 1) / 3) * 3 + 1;

    LocalDateTime startOfQuarter = LocalDate.of(currentYear, firstMonthOfQuarter, 1).atStartOfDay();
    LocalDateTime now = LocalDateTime.now();

    boolean alreadyRanThisQuarter = operationLogRepository.existsByActionAndTimestampBetween(
        "SYSTEM_EXPIRE_OVERTIME", startOfQuarter, now);

    if (!alreadyRanThisQuarter) {
      scheduleService.processOvertimeExpirations();
    }
  }
}
