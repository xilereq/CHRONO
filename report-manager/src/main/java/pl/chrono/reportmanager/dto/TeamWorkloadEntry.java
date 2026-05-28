package pl.chrono.reportmanager.dto;

public record TeamWorkloadEntry(
    Integer employeeId,
    String employeeFullName,
    Long totalWorkSeconds,
    Long totalRequiredSeconds,
    Long totalBalanceSeconds
) {}
