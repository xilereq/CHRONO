package pl.chrono.notificationmanager.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.transaction.annotation.Transactional;
import pl.chrono.applicationmanager.event.ApplicationNotificationEvent;
import pl.chrono.dbmanager.model.Notification;
import pl.chrono.dbmanager.model.User;
import pl.chrono.dbmanager.repository.NotificationRepository;
import pl.chrono.dbmanager.repository.UserRepository;
import pl.chrono.notificationmanager.TestConfig;
import pl.chrono.usermanager.event.PasswordResetEvent;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

@SpringBootTest(
    classes = TestConfig.class,
    properties = {
        "spring.mail.username=no-reply@chrono.pl"
    }
)
@Transactional
class NotificationManagerIT {

  @Autowired
  private ApplicationEventPublisher eventPublisher;

  @Autowired
  private NotificationService notificationService;

  @Autowired
  private NotificationRepository notificationRepository;

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private JavaMailSender javaMailSender;

  private User testEmployee;
  private User testManager;

  @BeforeEach
  void setUp() {
    Mockito.reset(javaMailSender);

    userRepository.deleteAll();

    testEmployee = new User();
    testEmployee.setEmail("employee@chrono.pl");
    testEmployee.setName("Jan");
    testEmployee.setSurname("Kowalski");
    testEmployee.setPasswordHash("hash123");
    testEmployee = userRepository.save(testEmployee);

    testManager = new User();
    testManager.setEmail("manager@chrono.pl");
    testManager.setName("Anna");
    testManager.setSurname("Zarzadca");
    testManager.setPasswordHash("hash123");
    testManager = userRepository.save(testManager);
  }

  @Test
  void shouldHandleApplicationCreatedEvent_AndSaveNotification_AndSendEmail() {
    // given
    Long applicationId = 101L;
    ApplicationNotificationEvent event = new ApplicationNotificationEvent(
        applicationId,
        testEmployee.getUserId(),
        testManager.getUserId(),
        "CREATED",
        null,
        null
    );

    eventPublisher.publishEvent(event);

    List<Notification> unreadNotifications = notificationService.getUnreadNotifications(testManager.getUserId());
    assertThat(unreadNotifications).hasSize(1);

    Notification notification = unreadNotifications.get(0);
    assertThat(notification.getUserId()).isEqualTo(testManager.getUserId());
    assertThat(notification.getType()).isEqualTo("APPLICATION_CREATED");
    assertThat(notification.getReferenceId()).isEqualTo(applicationId);
    assertThat(notification.isRead()).isFalse();

    ArgumentCaptor<SimpleMailMessage> mailCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);
    verify(javaMailSender, timeout(2000).times(1)).send(mailCaptor.capture());

    SimpleMailMessage sentEmail = mailCaptor.getValue();
    assertThat(sentEmail.getTo()).containsExactly("manager@chrono.pl");
    assertThat(sentEmail.getSubject()).isEqualTo("Nowy wniosek oczekuje na Twoją decyzję");
    assertThat(sentEmail.getText()).contains("Złożono nowy wniosek");
  }

  @Test
  void shouldHandleApplicationStatusChangedEvent_AndSaveNotification_AndSendEmail() {
    Long applicationId = 102L;
    ApplicationNotificationEvent event = new ApplicationNotificationEvent(
        applicationId,
        testEmployee.getUserId(),
        testManager.getUserId(),
        "STATUS_CHANGED",
        "ACCEPTED",
        "Anna Zarzadca"
    );

    eventPublisher.publishEvent(event);

    List<Notification> unreadNotifications = notificationService.getUnreadNotifications(testEmployee.getUserId());
    assertThat(unreadNotifications).hasSize(1);

    Notification notification = unreadNotifications.get(0);
    assertThat(notification.getUserId()).isEqualTo(testEmployee.getUserId());
    assertThat(notification.getType()).isEqualTo("APPLICATION_STATUS_CHANGED");
    assertThat(notification.getMessage()).contains("ACCEPTED").contains("Anna Zarzadca");

    verify(javaMailSender, timeout(2000).times(1)).send(any(SimpleMailMessage.class));
  }

  @Test
  void shouldHandlePasswordResetEvent_AndSendEmail() {
    PasswordResetEvent event = new PasswordResetEvent(testEmployee.getUserId(), testEmployee.getEmail(), "secret-reset-token-123");

    eventPublisher.publishEvent(event);

    ArgumentCaptor<SimpleMailMessage> mailCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);
    verify(javaMailSender, timeout(2000).times(1)).send(mailCaptor.capture());

    SimpleMailMessage sentEmail = mailCaptor.getValue();
    assertThat(sentEmail.getTo()).containsExactly("employee@chrono.pl");
    assertThat(sentEmail.getSubject()).isEqualTo("Resetowanie hasła w systemie Chrono");
    assertThat(sentEmail.getText()).contains("secret-reset-token-123");
  }

  @Test
  void shouldMarkNotificationAsRead() {
    notificationService.sendNotification(testEmployee.getUserId(), "Test message", 1L, "TEST_REF", "TEST_TYPE");
    List<Notification> initialUnread = notificationService.getUnreadNotifications(testEmployee.getUserId());
    assertThat(initialUnread).hasSize(1);

    Long notificationId = initialUnread.get(0).getId();

    notificationService.markAsRead(notificationId);

    List<Notification> unreadAfterMark = notificationService.getUnreadNotifications(testEmployee.getUserId());
    assertThat(unreadAfterMark).isEmpty();

    Notification dbNotification = notificationRepository.findById(notificationId).orElseThrow();
    assertThat(dbNotification.isRead()).isTrue();
  }
}
