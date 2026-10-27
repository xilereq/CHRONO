package pl.chrono.notificationmanager.service;

import jakarta.mail.internet.MimeMessage;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import pl.chrono.dbmanager.repository.UserRepository;
import pl.chrono.reportmanager.event.ReportAttachmentDto;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailNotificationService {

  private final JavaMailSender mailSender;
  private final UserRepository userRepository;

  @Value("${spring.mail.username}")
  private String senderEmail;

  @Async
  public void sendEmailNotification(Integer userId, String message, String type) {
    userRepository.findById(userId).ifPresent(user -> {
      try {
        SimpleMailMessage mailMessage = new SimpleMailMessage();
        mailMessage.setFrom(senderEmail);
        mailMessage.setTo(user.getEmail());

        mailMessage.setSubject(resolveSubject(type));
        mailMessage.setText(message);

        mailSender.send(mailMessage);
        log.info("Wysłano powiadomienie e-mail do użytkownika: {}", user.getEmail());
      } catch (Exception e){
        log.error("Nie udało się wysłać e-maila do użytkownika o ID: " + userId, e);
      }
    });
  }

  public void sendReportEmailWithAttachments(
      String[] recipientEmails,
      String subject,
      String bodyText,
      List<ReportAttachmentDto> attachments) throws Exception {

    MimeMessage mimeMessage = mailSender.createMimeMessage();
    MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

    helper.setFrom(senderEmail);
    helper.setTo(recipientEmails);
    helper.setSubject(subject);
    helper.setText(bodyText, false);

    for (ReportAttachmentDto attachment : attachments) {
      helper.addAttachment(
          attachment.fileName(),
          new ByteArrayResource(attachment.content()),
          attachment.contentType()
      );
    }

    mailSender.send(mimeMessage);
    log.info("Wysłano raport cykliczny '{}' do: {}", subject, String.join(", ", recipientEmails));
  }

  private String resolveSubject(String type) {
    return switch (type) {
      case "APPLICATION_CREATED" -> "Nowy wniosek oczekuje na Twoją decyzję";
      case "APPLICATION_STATUS_CHANGED" -> "Zmiana statusu Twojego wniosku";
      case "VIOLATION_REST", "VIOLATION_MAX_WORK" -> "Ostrzeżenie o naruszeniu czasu pracy";
      case "PASSWORD_RESET" -> "Resetowanie hasła w systemie Chrono";
      case "SCHEDULED_REPORT_FAILED" -> "Błąd generowania cyklicznego raportu - Chrono";
      default -> "Nowe powiadomienie systemowe - Chrono";
    };
  }
}
