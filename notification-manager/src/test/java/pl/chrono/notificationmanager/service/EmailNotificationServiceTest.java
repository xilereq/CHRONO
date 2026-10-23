package pl.chrono.notificationmanager.service;

import jakarta.mail.Address;
import jakarta.mail.Message;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;
import pl.chrono.dbmanager.model.User;
import pl.chrono.dbmanager.repository.UserRepository;
import pl.chrono.reportmanager.event.ReportAttachmentDto;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
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

  @Test
  void sendEmailNotification_ShouldUseScheduledReportFailedSubject_WhenTypeIsScheduledReportFailed() {
    User user = new User(); user.setEmail("test@test.pl");
    when(userRepository.findById(1)).thenReturn(Optional.of(user));

    emailNotificationService.sendEmailNotification(1, "Msg", "SCHEDULED_REPORT_FAILED");

    ArgumentCaptor<SimpleMailMessage> messageCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);
    verify(mailSender, times(1)).send(messageCaptor.capture());
    assertEquals("Błąd generowania cyklicznego raportu - Chrono", messageCaptor.getValue().getSubject());
  }

  // --- sendReportEmailWithAttachments (używane przez raporty cykliczne) ---

  private MimeMessage stubMimeMessage() {
    MimeMessage mimeMessage = new MimeMessage(Session.getInstance(new Properties()));
    when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
    return mimeMessage;
  }

  private static Map<String, byte[]> extractAttachments(MimeMessage message) throws Exception {
    MimeMultipart root = (MimeMultipart) message.getContent();
    Map<String, byte[]> attachments = new LinkedHashMap<>();
    for (int i = 0; i < root.getCount(); i++) {
      MimeBodyPart part = (MimeBodyPart) root.getBodyPart(i);
      if (part.getFileName() != null) {
        attachments.put(part.getFileName(), part.getInputStream().readAllBytes());
      }
    }
    return attachments;
  }

  @Test
  void sendReportEmailWithAttachments_ShouldBuildAndSendMessage_WithAllAttachments() throws Exception {
    MimeMessage mimeMessage = stubMimeMessage();
    String[] recipients = {"jan@chrono.pl", "anna@chrono.pl"};
    byte[] pdfBytes = {1, 2, 3};
    byte[] csvBytes = "id,hours".getBytes(StandardCharsets.UTF_8);
    List<ReportAttachmentDto> attachments = List.of(
        new ReportAttachmentDto("timesheet.pdf", pdfBytes, "application/pdf"),
        new ReportAttachmentDto("overtime.csv", csvBytes, "text/csv"));

    emailNotificationService.sendReportEmailWithAttachments(
        recipients, "[Chrono] Raport cykliczny", "Treść maila", attachments);

    verify(mailSender, times(1)).send(mimeMessage);

    assertEquals("no-reply@chrono.pl", mimeMessage.getFrom()[0].toString());
    assertEquals("[Chrono] Raport cykliczny", mimeMessage.getSubject());

    Address[] to = mimeMessage.getRecipients(Message.RecipientType.TO);
    assertEquals(2, to.length);
    assertEquals("jan@chrono.pl", to[0].toString());
    assertEquals("anna@chrono.pl", to[1].toString());

    Map<String, byte[]> sentAttachments = extractAttachments(mimeMessage);
    assertEquals(List.of("timesheet.pdf", "overtime.csv"), new ArrayList<>(sentAttachments.keySet()));
    assertArrayEquals(pdfBytes, sentAttachments.get("timesheet.pdf"));
    assertArrayEquals(csvBytes, sentAttachments.get("overtime.csv"));
  }

  @Test
  void sendReportEmailWithAttachments_ShouldSendMessageWithoutAttachments_WhenListIsEmpty() throws Exception {
    MimeMessage mimeMessage = stubMimeMessage();

    emailNotificationService.sendReportEmailWithAttachments(
        new String[] {"jan@chrono.pl"}, "Temat", "Treść", List.of());

    verify(mailSender, times(1)).send(mimeMessage);
    assertEquals("Temat", mimeMessage.getSubject());
    assertTrue(extractAttachments(mimeMessage).isEmpty());
  }

  @Test
  void sendReportEmailWithAttachments_ShouldPropagateException_WhenMailSenderFails() {
    MimeMessage mimeMessage = stubMimeMessage();
    doThrow(new MailSendException("SMTP Server down")).when(mailSender).send(any(MimeMessage.class));

    MailSendException thrown = assertThrows(MailSendException.class, () ->
        emailNotificationService.sendReportEmailWithAttachments(
            new String[] {"jan@chrono.pl"}, "Temat", "Treść", List.of()));

    assertEquals("SMTP Server down", thrown.getMessage());
    verify(mailSender, times(1)).send(mimeMessage);
  }
}
