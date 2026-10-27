package pl.chrono.notificationmanager.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import pl.chrono.applicationmanager.event.ApplicationNotificationEvent;
import pl.chrono.timemanager.api.I_GoogleCalendarService;

@Slf4j
@Component
@RequiredArgsConstructor
public class GoogleCalendarListener {

  private static final String ACTION_STATUS_CHANGED = "STATUS_CHANGED";
  private static final String STATUS_APPROVED = "APPROVED";

  private final I_GoogleCalendarService googleCalendarService;

  @Async
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void handleApplicationApproved(ApplicationNotificationEvent event) {
    if (!ACTION_STATUS_CHANGED.equals(event.action())
        || !STATUS_APPROVED.equals(event.newStatus())) {
      return;
    }

    try {
      googleCalendarService.syncApplication(event.applicationId());
    } catch (Exception e) {
      log.warn("Failed to sync application {} with Google Calendar: {}",
          event.applicationId(), e.getMessage());
    }
  }
}
