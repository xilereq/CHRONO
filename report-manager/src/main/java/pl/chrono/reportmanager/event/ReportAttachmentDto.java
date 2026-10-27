package pl.chrono.reportmanager.event;

public record ReportAttachmentDto(
    String fileName,
    byte[] content,
    String contentType
) {}
