package pl.chrono.applicationmanager;

import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import pl.chrono.applicationmanager.service.NotificationService;
import pl.chrono.dbmanager.model.Notification;
import pl.chrono.dbmanager.repository.NotificationRepository;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

  @Mock
  private NotificationRepository notificationRepository;

  @InjectMocks
  private NotificationService notificationService;

  @Test
  void subscribe_ShouldReturnEmitter() {
    SseEmitter emitter = notificationService.subscribe(1);
    assertNotNull(emitter);
  }

  @Test
  void sendNotification_ShouldSaveToDatabase() {
    Notification mockNotification = new Notification();
    when(notificationRepository.save(any(Notification.class))).thenReturn(mockNotification);

    notificationService.sendNotification(1, "Message", 100L, "TYPE");

    verify(notificationRepository, times(1)).save(any(Notification.class));
  }

  @Test
  void sendNotification_ShouldSendToEmitter_WhenUserIsSubscribed() {
    notificationService.subscribe(2);
    when(notificationRepository.save(any(Notification.class))).thenReturn(new Notification());

    assertDoesNotThrow(() -> notificationService.sendNotification(2, "Test", 1L, "TYPE"));
  }

  @Test
  void getUnreadNotifications_ShouldReturnList() {
    when(notificationRepository.findByUserIdAndIsReadFalseOrderByCreatedAtDesc(1))
        .thenReturn(List.of(new Notification()));

    List<Notification> result = notificationService.getUnreadNotifications(1);
    assertEquals(1, result.size());
  }

  @Test
  void markAsRead_ShouldUpdateAndSave_WhenNotificationExists() {
    Notification notification = new Notification();
    notification.setRead(false);
    when(notificationRepository.findById(1L)).thenReturn(Optional.of(notification));

    notificationService.markAsRead(1L);

    assertTrue(notification.isRead());
    verify(notificationRepository, times(1)).save(notification);
  }
}
