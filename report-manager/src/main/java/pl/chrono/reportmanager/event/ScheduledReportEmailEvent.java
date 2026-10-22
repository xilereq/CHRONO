package pl.chrono.reportmanager.event;

import java.util.List;

public record ScheduledReportEmailEvent(
    String[] recipientEmails,
    String subject,
    String body,
    List<ReportAttachmentDto> attachments
) {}
