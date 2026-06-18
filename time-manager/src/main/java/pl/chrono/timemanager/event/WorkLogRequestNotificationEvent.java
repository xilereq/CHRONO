package pl.chrono.timemanager.event;

public record WorkLogRequestNotificationEvent(
    Long requestId,
    Integer employeeId,
    Integer managerId,
    String action,
    String status,
    String reviewerName
) {
}
