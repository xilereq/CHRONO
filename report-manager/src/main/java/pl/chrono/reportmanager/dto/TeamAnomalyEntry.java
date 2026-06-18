package pl.chrono.reportmanager.dto;

public record TeamAnomalyEntry(
    Integer employeeId,
    String employeeFullName,
    Long totalWorkSeconds,
    Long totalRequiredSeconds,
    Long balanceSeconds,
    String anomalyType
) {}
