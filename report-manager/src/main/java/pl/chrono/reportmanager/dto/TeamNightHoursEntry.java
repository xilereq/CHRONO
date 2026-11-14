package pl.chrono.reportmanager.dto;

public record TeamNightHoursEntry(
    Integer employeeId,
    String employeeFullName,
    Integer nightShifts,
    Long nightSeconds
) {}
