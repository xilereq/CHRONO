package pl.chrono.notificationmanager.listener;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pl.chrono.applicationmanager.event.ApplicationNotificationEvent;
import pl.chrono.timemanager.api.I_GoogleCalendarService;

@ExtendWith(MockitoExtension.class)
class GoogleCalendarListenerTest {

  @Mock
  private I_GoogleCalendarService googleCalendarService;

  @InjectMocks
  private GoogleCalendarListener googleCalendarListener;

  @Test
  void handleApplicationApprovedCallsSyncWhenApprovedTest() {
    ApplicationNotificationEvent event = new ApplicationNotificationEvent(
        15L, 2, 1, "STATUS_CHANGED", "APPROVED", "Jan Kowalski"
    );

    googleCalendarListener.handleApplicationApproved(event);

    verify(googleCalendarService).syncApplication(15L);
  }

  @Test
  void handleApplicationApprovedSkipsWhenActionIsNotStatusChangedTest() {
    ApplicationNotificationEvent event = new ApplicationNotificationEvent(
        15L, 2, 1, "CREATED", "APPROVED", null
    );

    googleCalendarListener.handleApplicationApproved(event);

    verifyNoInteractions(googleCalendarService);
  }

  @Test
  void handleApplicationApprovedSkipsWhenStatusIsNotApprovedTest() {
    ApplicationNotificationEvent event = new ApplicationNotificationEvent(
        15L, 2, 1, "STATUS_CHANGED", "REJECTED", "Jan Kowalski"
    );

    googleCalendarListener.handleApplicationApproved(event);

    verifyNoInteractions(googleCalendarService);
  }

  @Test
  void handleApplicationApprovedSwallowsServiceExceptionTest() {
    ApplicationNotificationEvent event = new ApplicationNotificationEvent(
        20L, 2, 1, "STATUS_CHANGED", "APPROVED", "Jan Kowalski"
    );
    doThrow(new RuntimeException("Google API unavailable"))
        .when(googleCalendarService)
        .syncApplication(20L);

    assertDoesNotThrow(() -> googleCalendarListener.handleApplicationApproved(event));

    verify(googleCalendarService).syncApplication(20L);
  }
}
