package pl.chrono.notificationmanager.listener;

import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import pl.chrono.applicationmanager.event.ApplicationNotificationEvent;
import pl.chrono.dbmanager.repository.WorkLogRequestRepository;
import pl.chrono.notificationmanager.service.NotificationService;
import pl.chrono.timemanager.event.WorkLogRequestNotificationEvent;

@Component
@RequiredArgsConstructor
public class NotificationListener {

  private final NotificationService notificationService;
  private final WorkLogRequestRepository workLogRequestRepository;

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

  @EventListener
  public void handleWorkLogRequestNotification(WorkLogRequestNotificationEvent event) {
    String date = workLogRequestRepository.findById(event.requestId())
        .get()
        .getProposedStartTime()
        .toLocalDate()
        .toString();
    if ("CREATED".equals(event.action()) && event.managerId() != null) {
      String message = "Złożono nową prośbę o korektę czasu pracy z dnia " + date + " oczekującą na Twoją decyzję.";

      notificationService.sendNotification(
          event.managerId(),
          message,
          event.requestId(),
          "WORK_LOG_REQUEST",
          "WORK_LOG_REQUEST_CREATED"
      );

    } else if ("STATUS_CHANGED".equals(event.action())) {
      String reviewer = event.reviewerName() != null ? event.reviewerName() : "System";
      String message = "Twoja prośba o korektę czasu pracy z dnia " + date + " otrzymała nowy status: " + event.status() + " od " + reviewer;

      notificationService.sendNotification(
          event.employeeId(),
          message,
          event.requestId(),
          "WORK_LOG_REQUEST",
          "WORK_LOG_REQUEST_STATUS_CHANGED"
      );
    }
  }
}
