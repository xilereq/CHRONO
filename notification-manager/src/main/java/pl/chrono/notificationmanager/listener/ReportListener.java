package pl.chrono.notificationmanager.listener;

import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import pl.chrono.notificationmanager.service.EmailNotificationService;
import pl.chrono.notificationmanager.service.NotificationService;
import pl.chrono.reportmanager.event.ScheduledReportEmailEvent;
import pl.chrono.reportmanager.event.ScheduledReportFailureEvent;

@Component
@RequiredArgsConstructor
public class ReportListener {

  private final NotificationService notificationService;
  private final EmailNotificationService emailNotificationService;

  @EventListener
  public void handleScheduledReportEmail(ScheduledReportEmailEvent event) throws Exception {
    emailNotificationService.sendReportEmailWithAttachments(
        event.recipientEmails(),
        event.subject(),
        event.body(),
        event.attachments()
    );
  }

  @EventListener
  public void handleScheduledReportFailure(ScheduledReportFailureEvent event) {
    notificationService.sendNotification(
        event.creatorId(),
        event.errorMessage(),
        event.scheduleId(),
        "SCHEDULED_REPORT",
        "SCHEDULED_REPORT_FAILED"
    );
  }
}
