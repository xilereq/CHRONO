package pl.chrono.applicationmanager.event;

public record ApplicationNotificationEvent(
    Long applicationId,
    Integer employeeId,
    Integer managerId,
    String action,
    String newStatus,
    String reviewerName
) {}
