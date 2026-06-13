package pl.chrono.notificationmanager.listener;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pl.chrono.applicationmanager.event.ApplicationNotificationEvent;
import pl.chrono.notificationmanager.service.NotificationService;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationListenerTest {

  @Mock
  private NotificationService notificationService;

  @InjectMocks
  private NotificationListener notificationListener;

  @Test
  void handleApplicationNotification_ShouldSendToManager_WhenCreated() {
    ApplicationNotificationEvent event = mock(ApplicationNotificationEvent.class);
    when(event.action()).thenReturn("CREATED");
    when(event.managerId()).thenReturn(2);
    when(event.applicationId()).thenReturn(100L);

    notificationListener.handleApplicationNotification(event);

    verify(notificationService, times(1)).sendNotification(
        eq(2),
        argThat(msg -> msg.contains("100")), // Sprawdza czy wiadomość zawiera ID wniosku
        eq(100L),
        eq("APPLICATION"),
        eq("APPLICATION_CREATED")
    );
  }

  @Test
  void handleApplicationNotification_ShouldDoNothing_WhenCreatedButNoManager() {
    ApplicationNotificationEvent event = mock(ApplicationNotificationEvent.class);
    when(event.action()).thenReturn("CREATED");
    when(event.managerId()).thenReturn(null);

    notificationListener.handleApplicationNotification(event);

    verify(notificationService, never()).sendNotification(any(), anyString(), anyLong(), anyString(), anyString());
  }

  @Test
  void handleApplicationNotification_ShouldSendToEmployee_WhenStatusChanged() {
    ApplicationNotificationEvent event = mock(ApplicationNotificationEvent.class);
    when(event.action()).thenReturn("STATUS_CHANGED");
    when(event.employeeId()).thenReturn(1);
    when(event.applicationId()).thenReturn(100L);
    when(event.newStatus()).thenReturn("ZAAKCEPTOWANY");
    when(event.reviewerName()).thenReturn("Jan Kowalski");

    notificationListener.handleApplicationNotification(event);

    verify(notificationService, times(1)).sendNotification(
        eq(1),
        argThat(msg -> msg.contains("ZAAKCEPTOWANY") && msg.contains("Jan Kowalski")),
        eq(100L),
        eq("APPLICATION"),
        eq("APPLICATION_STATUS_CHANGED")
    );
  }

  @Test
  void handleApplicationNotification_ShouldDoNothing_WhenActionIsUnknown() {
    ApplicationNotificationEvent event = mock(ApplicationNotificationEvent.class);
    when(event.action()).thenReturn("UNKNOWN_ACTION");

    notificationListener.handleApplicationNotification(event);

    verify(notificationService, never()).sendNotification(any(), anyString(), anyLong(), anyString(), anyString());
  }
}
