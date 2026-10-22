package pl.chrono.reportmanager.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import pl.chrono.reportmanager.service.ScheduledReportService;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReportSchedulerJob {

  private final ScheduledReportService scheduledReportService;

  @EventListener(ApplicationReadyEvent.class)
  public void checkOverdueReportsOnStartup() {
    log.info("Start serwera: sprawdzanie zaległych raportów cyklicznych...");
    scheduledReportService.processDueSchedules();
  }

  @Scheduled(cron = "${chrono.reports.scheduler.cron:0 0 * * * *}")
  public void executeScheduledReports() {
    scheduledReportService.processDueSchedules();
  }
}
