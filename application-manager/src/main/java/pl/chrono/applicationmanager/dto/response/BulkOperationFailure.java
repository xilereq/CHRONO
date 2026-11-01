package pl.chrono.applicationmanager.dto.response;

public record BulkOperationFailure(
    Long applicationId,
    String errorCode,
    String message
) {}
