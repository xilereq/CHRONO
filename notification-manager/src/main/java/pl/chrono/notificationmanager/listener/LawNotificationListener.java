package pl.chrono.notificationmanager.listener;

import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import pl.chrono.notificationmanager.service.NotificationService;
import pl.chrono.timemanager.event.LawViolationEvent;

@Component
@RequiredArgsConstructor
public class LawNotificationListener {

  private final NotificationService notificationService;

  @EventListener
  public void handleLaborLawViolation(LawViolationEvent event) {
    notificationService.sendNotification(
        event.userId(),
        event.message(),
        event.referenceId(),
        event.referenceType(),
        event.violationType()
    );
  }
}
