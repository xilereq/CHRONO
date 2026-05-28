package pl.chrono.applicationmanager.dto.request;

public record ChangeApplicationStatusRequest(
    String newStatus,
    String decisionReason
) {}
