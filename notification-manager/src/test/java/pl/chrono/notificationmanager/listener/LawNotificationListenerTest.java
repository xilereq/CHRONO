package pl.chrono.notificationmanager.listener;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pl.chrono.notificationmanager.service.NotificationService;
import pl.chrono.timemanager.event.LawViolationEvent;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LawNotificationListenerTest {

  @Mock
  private NotificationService notificationService;

  @InjectMocks
  private LawNotificationListener lawNotificationListener;

  @Test
  void handleLaborLawViolation_ShouldForwardEventToNotificationService() {
    LawViolationEvent event = mock(LawViolationEvent.class);
    when(event.userId()).thenReturn(1);
    when(event.message()).thenReturn("Naruszenie");
    when(event.referenceId()).thenReturn(100L);
    when(event.referenceType()).thenReturn("LOG");
    when(event.violationType()).thenReturn("VIOLATION_REST");

    lawNotificationListener.handleLaborLawViolation(event);

    verify(notificationService, times(1)).sendNotification(
        1,
        "Naruszenie",
        100L,
        "LOG",
        "VIOLATION_REST"
    );
  }
}
