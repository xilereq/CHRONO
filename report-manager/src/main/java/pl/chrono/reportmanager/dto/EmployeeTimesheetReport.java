package pl.chrono.reportmanager.dto;

import java.util.List;

public record EmployeeTimesheetReport(
    Integer employeeId,
    String employeeFullName,
    int year,
    int month,
    Long totalWorkSeconds,
    Long totalRequiredSeconds,
    Long totalBalanceSeconds,
    List<DailyTimesheetEntry> dailyEntries
) {}
