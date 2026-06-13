package pl.chrono.notificationmanager.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;
import pl.chrono.dbmanager.model.User;
import pl.chrono.dbmanager.repository.UserRepository;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailNotificationServiceTest {

  @Mock private JavaMailSender mailSender;
  @Mock private UserRepository userRepository;

  @InjectMocks
  private EmailNotificationService emailNotificationService;

  @BeforeEach
  void setUp() {
    ReflectionTestUtils.setField(emailNotificationService, "senderEmail", "no-reply@chrono.pl");
  }

  @Test
  void sendEmailNotification_ShouldSendEmail_WhenUserExists() {
    // Arrange
    User user = new User(); user.setEmail("test@test.pl");
    when(userRepository.findById(1)).thenReturn(Optional.of(user));

    // Act
    emailNotificationService.sendEmailNotification(1, "Wiadomość testowa", "APPLICATION_CREATED");

    // Assert
    ArgumentCaptor<SimpleMailMessage> messageCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);
    verify(mailSender, times(1)).send(messageCaptor.capture());

    SimpleMailMessage sentMessage = messageCaptor.getValue();
    assertEquals("no-reply@chrono.pl", sentMessage.getFrom());
    assertEquals("test@test.pl", sentMessage.getTo()[0]);
    assertEquals("Nowy wniosek oczekuje na Twoją decyzję", sentMessage.getSubject());
    assertEquals("Wiadomość testowa", sentMessage.getText());
  }

  @Test
  void sendEmailNotification_ShouldResolveAllSubjectsCorrectly() {
    User user = new User(); user.setEmail("test@test.pl");
    when(userRepository.findById(1)).thenReturn(Optional.of(user));

    String[] types = {
        "APPLICATION_STATUS_CHANGED",
        "VIOLATION_REST",
        "VIOLATION_MAX_WORK",
        "PASSWORD_RESET",
        "UNKNOWN_TYPE"
    };

    String[] expectedSubjects = {
        "Zmiana statusu Twojego wniosku",
        "Ostrzeżenie o naruszeniu czasu pracy",
        "Ostrzeżenie o naruszeniu czasu pracy",
        "Resetowanie hasła w systemie Chrono",
        "Nowe powiadomienie systemowe - Chrono"
    };

    for (int i = 0; i < types.length; i++) {
      emailNotificationService.sendEmailNotification(1, "Msg", types[i]);
    }

    ArgumentCaptor<SimpleMailMessage> messageCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);
    verify(mailSender, times(5)).send(messageCaptor.capture());

    for (int i = 0; i < types.length; i++) {
      assertEquals(expectedSubjects[i], messageCaptor.getAllValues().get(i).getSubject());
    }
  }

  @Test
  void sendEmailNotification_ShouldDoNothing_WhenUserDoesNotExist() {
    when(userRepository.findById(1)).thenReturn(Optional.empty());

    emailNotificationService.sendEmailNotification(1, "Msg", "ANY");

    verify(mailSender, never()).send(any(SimpleMailMessage.class));
  }

  @Test
  void sendEmailNotification_ShouldCatchException_WhenMailSenderFails() {
    User user = new User(); user.setEmail("test@test.pl");
    when(userRepository.findById(1)).thenReturn(Optional.of(user));
    doThrow(new RuntimeException("SMTP Server down")).when(mailSender).send(any(SimpleMailMessage.class));

    emailNotificationService.sendEmailNotification(1, "Msg", "ANY");

    verify(mailSender, times(1)).send(any(SimpleMailMessage.class));
  }
}
