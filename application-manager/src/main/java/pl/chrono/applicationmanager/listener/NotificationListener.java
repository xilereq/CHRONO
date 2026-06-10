package pl.chrono.applicationmanager.listener;

import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import pl.chrono.applicationmanager.event.ApplicationNotificationEvent;
import pl.chrono.applicationmanager.service.NotificationService;

@Component
@RequiredArgsConstructor
public class NotificationListener {

  private final NotificationService notificationService;

  @EventListener
  public void handleApplicationNotification(ApplicationNotificationEvent event) {
    if ("CREATED".equals(event.action()) && event.managerId() != null) {
      String message = "Złożono nowy wniosek (ID: " + event.applicationId() + ") oczekujący na Twoją decyzję.";

      notificationService.sendNotification(
          event.managerId(),
          message,
          event.applicationId(),
          "APPLICATION",
          "APPLICATION_CREATED"
      );

    } else if ("STATUS_CHANGED".equals(event.action())) {
      String message = "Twój wniosek (ID: " + event.applicationId() + ") otrzymał nowy status: " + event.newStatus() + " od " + event.reviewerName();

      notificationService.sendNotification(
          event.employeeId(),
          message,
          event.applicationId(),
          "APPLICATION",
          "APPLICATION_STATUS_CHANGED"
      );
    }
  }
}
