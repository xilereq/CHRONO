package pl.chrono.reportmanager.dto;

import java.time.LocalDate;

public record TeamDailyOvertimeEntry(
    LocalDate date,
    Integer employeesWithOvertime,
    Long totalOvertimeSeconds
) {}
