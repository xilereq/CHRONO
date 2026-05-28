package pl.chrono.workforceapi.controller;

import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import pl.chrono.applicationmanager.service.NotificationService;
import pl.chrono.dbmanager.model.Notification;
import pl.chrono.usermanager.service.AuthService;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController extends BaseController {

  private final NotificationService notificationService;

  public NotificationController(AuthService authService, NotificationService notificationService) {
    super(authService);
    this.notificationService = notificationService;
  }

  @GetMapping(value = "/subscribe", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
  public SseEmitter subscribe(@RequestHeader("Authorization") String token) {
    return notificationService.subscribe(extractId(token));
  }

  @GetMapping("/unread")
  public ResponseEntity<List<Notification>> getUnreadNotifications(@RequestHeader("Authorization") String token) {
    return ResponseEntity.ok(notificationService.getUnreadNotifications(extractId(token)));
  }

  @PatchMapping("/{id}/read")
  public ResponseEntity<Void> markAsRead(@PathVariable Long id) {
    notificationService.markAsRead(id);
    return ResponseEntity.ok().build();
  }
}
