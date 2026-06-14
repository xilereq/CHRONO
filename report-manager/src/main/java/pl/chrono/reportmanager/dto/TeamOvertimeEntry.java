package pl.chrono.reportmanager.dto;

public record TeamOvertimeEntry(
    Integer employeeId,
    String employeeFullName,
    Long totalOvertimeSeconds
) {}
