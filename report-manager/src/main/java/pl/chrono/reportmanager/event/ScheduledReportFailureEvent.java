package pl.chrono.reportmanager.event;

public record ScheduledReportFailureEvent(
    Integer creatorId,
    Long scheduleId,
    String errorMessage
) {}
