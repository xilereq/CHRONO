package pl.chrono.notificationmanager.service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import pl.chrono.dbmanager.model.Notification;
import pl.chrono.dbmanager.repository.NotificationRepository;
import pl.chrono.usermanager.event.PasswordResetEvent;

@Service
@RequiredArgsConstructor
public class NotificationService {
  private final NotificationRepository notificationRepository;
  private final EmailNotificationService emailNotificationService;

  private final Map<Integer, SseEmitter> emitters = new ConcurrentHashMap<>();

  public SseEmitter subscribe(Integer userId) {
    SseEmitter emitter = new SseEmitter(60 * 60 * 1000L);
    emitters.put(userId, emitter);

    emitter.onCompletion(() -> emitters.remove(userId));
    emitter.onTimeout(() -> emitters.remove(userId));
    emitter.onError((e) -> emitters.remove(userId));

    return emitter;
  }

  @Transactional
  public void sendNotification(Integer userId, String message, Long referenceId,
                               String referenceType, String type) {
    Notification notification = new Notification();
    notification.setUserId(userId);
    notification.setMessage(message);
    notification.setReferenceId(referenceId);
    notification.setReferenceType(referenceType);
    notification.setType(type);
    Notification saved = notificationRepository.save(notification);

    SseEmitter emitter = emitters.get(userId);
    if (emitter != null) {
      try {
        emitter.send(SseEmitter.event().name("notification").data(saved));
      } catch (java.io.IOException e) {
        emitters.remove(userId);
      }
    }

    emailNotificationService.sendEmailNotification(userId, message, type);
  }

  @Transactional(readOnly = true)
  public List<Notification> getUnreadNotifications(Integer userId) {
    return notificationRepository.findByUserIdAndIsReadFalseOrderByCreatedAtDesc(userId);
  }

  @Transactional
  public void markAsRead(Long notificationId) {
    notificationRepository.findById(notificationId).ifPresent(n -> {
      n.setRead(true);
      notificationRepository.save(n);
    });
  }

  @EventListener
  public void handlePasswordRequestNotification(PasswordResetEvent event){
    String resetLink = "http://localhost:3000/reset-password?token=" + event.resetToken();
    String message = "Otrzymaliśmy prośbę o reset hasła. Aby ustawić nowe hasło, kliknij w poniższy link:\n\n" + resetLink;

    emailNotificationService.sendEmailNotification(
        event.userId(),
        message,
        "PASSWORD_RESET"
    );}
}
