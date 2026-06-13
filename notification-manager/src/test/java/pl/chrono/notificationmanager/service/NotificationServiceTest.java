package pl.chrono.notificationmanager.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import pl.chrono.dbmanager.model.Notification;
import pl.chrono.dbmanager.repository.NotificationRepository;
import pl.chrono.usermanager.event.PasswordResetEvent;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

  @Mock private NotificationRepository notificationRepository;
  @Mock private EmailNotificationService emailNotificationService;

  @InjectMocks
  private NotificationService notificationService;

  @Test
  void subscribe_ShouldReturnEmitterAndPutItInMap() {
    SseEmitter emitter = notificationService.subscribe(1);
    assertNotNull(emitter);

    Map<Integer, SseEmitter> emitters = getEmittersMap();
    assertTrue(emitters.containsKey(1));
  }

  @Test
  void sendNotification_ShouldSaveAndSendEmail_WhenNoEmitterExists() {
    when(notificationRepository.save(any(Notification.class))).thenAnswer(i -> i.getArguments()[0]);

    notificationService.sendNotification(1, "Msg", 100L, "TYPE", "ACTION");

    verify(notificationRepository, times(1)).save(any(Notification.class));
    verify(emailNotificationService, times(1)).sendEmailNotification(1, "Msg", "ACTION");
  }

  @Test
  void sendNotification_ShouldRemoveEmitter_WhenIOExceptionOccurs() throws Exception {
    when(notificationRepository.save(any(Notification.class))).thenAnswer(i -> i.getArguments()[0]);

    SseEmitter mockEmitter = mock(SseEmitter.class);

    doThrow(new IOException("Broken pipe"))
        .when(mockEmitter).send(any(SseEmitter.SseEventBuilder.class));

    Map<Integer, SseEmitter> emitters = getEmittersMap();
    emitters.put(1, mockEmitter);

    notificationService.sendNotification(1, "Msg", 100L, "TYPE", "ACTION");

    assertFalse(emitters.containsKey(1));
    verify(emailNotificationService, times(1)).sendEmailNotification(1, "Msg", "ACTION");
  }

  @Test
  void getUnreadNotifications_ShouldReturnListFromRepository() {
    when(notificationRepository.findByUserIdAndIsReadFalseOrderByCreatedAtDesc(1))
        .thenReturn(List.of(new Notification(), new Notification()));

    List<Notification> result = notificationService.getUnreadNotifications(1);

    assertEquals(2, result.size());
  }

  @Test
  void markAsRead_ShouldUpdateAndSave_WhenNotificationExists() {
    Notification notification = new Notification();
    notification.setRead(false);
    when(notificationRepository.findById(10L)).thenReturn(Optional.of(notification));

    notificationService.markAsRead(10L);

    assertTrue(notification.isRead());
    verify(notificationRepository, times(1)).save(notification);
  }

  @Test
  void markAsRead_ShouldDoNothing_WhenNotificationDoesNotExist() {
    when(notificationRepository.findById(10L)).thenReturn(Optional.empty());

    notificationService.markAsRead(10L);

    verify(notificationRepository, never()).save(any());
  }

  @Test
  void handlePasswordRequestNotification_ShouldFormatMessageAndSendEmail() {
    PasswordResetEvent event = mock(PasswordResetEvent.class);
    when(event.userId()).thenReturn(1);
    when(event.resetToken()).thenReturn("mocked_token");

    notificationService.handlePasswordRequestNotification(event);

    ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
    verify(emailNotificationService).sendEmailNotification(
        eq(1),
        messageCaptor.capture(),
        eq("PASSWORD_RESET")
    );

    assertTrue(messageCaptor.getValue().contains("mocked_token"));
    assertTrue(messageCaptor.getValue().contains("http://localhost:3000/reset-password?token="));
  }

  @SuppressWarnings("unchecked")
  private Map<Integer, SseEmitter> getEmittersMap() {
    return (Map<Integer, SseEmitter>) ReflectionTestUtils.getField(notificationService, "emitters");
  }
}
