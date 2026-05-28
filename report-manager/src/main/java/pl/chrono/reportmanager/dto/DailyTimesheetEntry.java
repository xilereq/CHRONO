package pl.chrono.reportmanager.dto;

import java.time.LocalDate;
import java.time.LocalTime;

public record DailyTimesheetEntry(
    LocalDate date,
    LocalTime startTime,
    LocalTime endTime,
    Long effectiveWorkSeconds,
    Integer requiredSeconds,
    Long balanceSeconds,
    String applicationType
) {}
