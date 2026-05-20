package pl.chrono.timemanager.dto.response;

import java.time.DayOfWeek;
import java.util.Set;

public record PolicyDto(
    String name,
    Integer dailyHours,
    Integer maxDailyHours,
    Integer maxWeeklyHours,
    Set<DayOfWeek> workingDays
) {}
