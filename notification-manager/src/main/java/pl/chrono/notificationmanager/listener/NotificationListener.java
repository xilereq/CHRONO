package pl.chrono.notificationmanager.listener;

import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import pl.chrono.applicationmanager.event.ApplicationNotificationEvent;
import pl.chrono.dbmanager.repository.WorkLogRequestRepository;
import pl.chrono.notificationmanager.service.NotificationService;
import pl.chrono.timemanager.event.WorkLogRequestNotificationEvent;
import pl.chrono.usermanager.api.I_ManagerDelegationService;
import pl.chrono.usermanager.event.ManagerDelegationEvent;

@Component
@RequiredArgsConstructor
public class NotificationListener {

  private final NotificationService notificationService;
  private final WorkLogRequestRepository workLogRequestRepository;
  private final I_ManagerDelegationService delegationService;

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
      notifyActiveDeputy(event.managerId(), event.employeeId(),
          "Złożono nowy wniosek (ID: " + event.applicationId()
              + ") oczekujący na decyzję. Jesteś zastępcą przełożonego tego pracownika.",
          event.applicationId(), "APPLICATION", "APPLICATION_CREATED");

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
      notifyActiveDeputy(event.managerId(), event.employeeId(),
          "Złożono nową prośbę o korektę czasu pracy z dnia " + date
              + " oczekującą na decyzję. Jesteś zastępcą przełożonego tego pracownika.",
          event.requestId(), "WORK_LOG_REQUEST", "WORK_LOG_REQUEST_CREATED");

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

  @EventListener
  public void handleManagerDelegation(ManagerDelegationEvent event) {
    String period = event.startDate() + " – " + event.endDate();
    if ("CREATED".equals(event.action())) {
      notificationService.sendNotification(
          event.deputyId(),
          "Zostałeś wyznaczony jako zastępca menedżera " + event.managerName()
              + " w okresie " + period + ". W tym czasie możesz rozpatrywać wnioski jego zespołu.",
          event.delegationId(),
          "MANAGER_DELEGATION",
          "MANAGER_DELEGATION_CREATED"
      );
    } else if ("REVOKED".equals(event.action())) {
      notificationService.sendNotification(
          event.deputyId(),
          "Zastępstwo za menedżera " + event.managerName() + " (" + period + ") zostało odwołane.",
          event.delegationId(),
          "MANAGER_DELEGATION",
          "MANAGER_DELEGATION_REVOKED"
      );
    }
  }

  private void notifyActiveDeputy(Integer managerId, Integer employeeId, String message,
                                  Long referenceId, String referenceType, String type) {
    delegationService.findActiveDeputyId(managerId)
        .filter(deputyId -> !deputyId.equals(employeeId))
        .ifPresent(deputyId -> notificationService.sendNotification(
            deputyId, message, referenceId, referenceType, type));
  }
}
