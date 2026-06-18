package pl.chrono.timemanager.event;

public record LawViolationEvent(
    Integer userId,
    String violationType,
    Long referenceId,
    String referenceType,
    String message
) {
}
