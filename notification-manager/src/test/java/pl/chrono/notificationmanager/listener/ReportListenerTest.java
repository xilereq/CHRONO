package pl.chrono.notificationmanager.listener;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pl.chrono.notificationmanager.service.EmailNotificationService;
import pl.chrono.notificationmanager.service.NotificationService;
import pl.chrono.reportmanager.event.ReportAttachmentDto;
import pl.chrono.reportmanager.event.ScheduledReportEmailEvent;
import pl.chrono.reportmanager.event.ScheduledReportFailureEvent;

@ExtendWith(MockitoExtension.class)
class ReportListenerTest {

  @Mock
  private NotificationService notificationService;

  @Mock
  private EmailNotificationService emailNotificationService;

  @InjectMocks
  private ReportListener reportListener;

  private ScheduledReportEmailEvent sampleEmailEvent(String[] recipients,
                                                     List<ReportAttachmentDto> attachments) {
    return new ScheduledReportEmailEvent(
        recipients,
        "[Chrono] Raport cykliczny: Raport tygodniowy (2026-10-12 - 2026-10-18)",
        "Cześć, w załączniku raport.",
        attachments
    );
  }

  @Test
  void handleScheduledReportEmailDelegatesToEmailServiceTest() throws Exception {
    String[] recipients = {"jan@chrono.pl", "anna@chrono.pl"};
    List<ReportAttachmentDto> attachments = List.of(
        new ReportAttachmentDto("report.pdf", new byte[] {1, 2, 3}, "application/pdf"));
    ScheduledReportEmailEvent event = sampleEmailEvent(recipients, attachments);

    reportListener.handleScheduledReportEmail(event);

    verify(emailNotificationService).sendReportEmailWithAttachments(
        same(recipients),
        eq(event.subject()),
        eq(event.body()),
        same(attachments)
    );
    verifyNoInteractions(notificationService);
  }

  @Test
  void handleScheduledReportEmailPassesEmptyAttachmentsTest() throws Exception {
    String[] recipients = {"jan@chrono.pl"};
    List<ReportAttachmentDto> attachments = List.of();
    ScheduledReportEmailEvent event = sampleEmailEvent(recipients, attachments);

    reportListener.handleScheduledReportEmail(event);

    verify(emailNotificationService).sendReportEmailWithAttachments(
        same(recipients),
        eq(event.subject()),
        eq(event.body()),
        same(attachments)
    );
  }

  @Test
  void handleScheduledReportEmailPropagatesSendingErrorTest() throws Exception {
    String[] recipients = {"jan@chrono.pl"};
    List<ReportAttachmentDto> attachments = List.of();
    ScheduledReportEmailEvent event = sampleEmailEvent(recipients, attachments);

    doThrow(new IOException("SMTP down"))
        .when(emailNotificationService)
        .sendReportEmailWithAttachments(
            same(recipients),
            eq(event.subject()),
            eq(event.body()),
            same(attachments));

    assertThatThrownBy(() -> reportListener.handleScheduledReportEmail(event))
        .isInstanceOf(IOException.class)
        .hasMessage("SMTP down");

    verifyNoInteractions(notificationService);
  }

  @Test
  void handleScheduledReportFailureSendsNotificationToCreatorTest() {
    ScheduledReportFailureEvent event = new ScheduledReportFailureEvent(
        5, 42L, "Nie udało się wygenerować raportu cyklicznego 'Raport tygodniowy' po 5 próbach.");

    reportListener.handleScheduledReportFailure(event);

    verify(notificationService).sendNotification(
        5,
        "Nie udało się wygenerować raportu cyklicznego 'Raport tygodniowy' po 5 próbach.",
        42L,
        "SCHEDULED_REPORT",
        "SCHEDULED_REPORT_FAILED"
    );
    verifyNoInteractions(emailNotificationService);
  }
}
