package pl.chrono.reportmanager.dto;

import java.time.LocalDate;
import java.util.List;

public record EmployeeTimesheetReport(
    Integer employeeId,
    String employeeFullName,
    LocalDate startDate,
    LocalDate endDate,
    Long totalWorkSeconds,
    Long totalRequiredSeconds,
    Long totalBalanceSeconds,
    Long totalNightSeconds,
    List<DailyTimesheetEntry> dailyEntries
) {}
