package pl.chrono.timemanager.service;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class WorkTaskScheduler {
  private final ScheduleService scheduleService;

  @Scheduled(fixedRate = 300000)
  public void runCloseTooLongWorkLogs() {
    scheduleService.closeTooLongWorkLogs();
  }

  @Scheduled(cron = "0 0 4 * * *")
  public void runCountOvertime() {
    scheduleService.countOvertime();
  }

  @EventListener(ApplicationReadyEvent.class)
  public void runOnStartup() {
    scheduleService.closeTooLongWorkLogs();
    scheduleService.countOvertime();
  }
}
