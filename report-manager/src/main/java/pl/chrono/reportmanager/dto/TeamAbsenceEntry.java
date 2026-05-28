package pl.chrono.reportmanager.dto;

import java.time.LocalDate;

public record TeamAbsenceEntry(
    Integer employeeId,
    String employeeFullName,
    String applicationType,
    String status,
    LocalDate startDate,
    LocalDate endDate
) {}
