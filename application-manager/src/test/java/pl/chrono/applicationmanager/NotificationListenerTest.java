package pl.chrono.applicationmanager;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import org.mockito.junit.jupiter.MockitoExtension;
import pl.chrono.applicationmanager.event.ApplicationNotificationEvent;
import pl.chrono.applicationmanager.listener.NotificationListener;
import pl.chrono.applicationmanager.service.NotificationService;

@ExtendWith(MockitoExtension.class)
class NotificationListenerTest {

  @Mock
  private NotificationService notificationService;

  @InjectMocks
  private NotificationListener notificationListener;

  @Test
  void handleApplicationNotification_ShouldSendToManager_WhenCreated() {
    Long applicationId = 100L;
    Integer managerId = 5;
    ApplicationNotificationEvent event = new ApplicationNotificationEvent(
        applicationId, 1, managerId, "CREATED", "PENDING", null
    );

    notificationListener.handleApplicationNotification(event);

    String expectedMessage = "Złożono nowy wniosek (ID: 100) oczekujący na Twoją decyzję.";
    verify(notificationService, times(1)).sendNotification(
        managerId, expectedMessage, applicationId, "APPLICATION_CREATED"
    );
  }

  @Test
  void handleApplicationNotification_ShouldNotSend_WhenCreatedAndManagerIsNull() {
    ApplicationNotificationEvent event = new ApplicationNotificationEvent(
        100L, 1, null, "CREATED", "PENDING", null
    );

    notificationListener.handleApplicationNotification(event);

    verifyNoInteractions(notificationService);
  }

  @Test
  void handleApplicationNotification_ShouldSendToEmployee_WhenStatusChanged() {
    Long applicationId = 200L;
    Integer employeeId = 3;
    String newStatus = "APPROVED";
    String reviewerName = "Jan Kowalski";
    ApplicationNotificationEvent event = new ApplicationNotificationEvent(
        applicationId, employeeId, 5, "STATUS_CHANGED", newStatus, reviewerName
    );

    notificationListener.handleApplicationNotification(event);

    String expectedMessage = "Twój wniosek (ID: 200) otrzymał nowy status: APPROVED od Jan Kowalski";
    verify(notificationService, times(1)).sendNotification(
        employeeId, expectedMessage, applicationId, "APPLICATION_STATUS_CHANGED"
    );
  }

  @Test
  void handleApplicationNotification_ShouldDoNothing_WhenUnknownAction() {
    ApplicationNotificationEvent event = new ApplicationNotificationEvent(
        100L, 1, 5, "UNKNOWN_ACTION", "PENDING", null
    );

    notificationListener.handleApplicationNotification(event);

    verifyNoInteractions(notificationService);
  }
}
