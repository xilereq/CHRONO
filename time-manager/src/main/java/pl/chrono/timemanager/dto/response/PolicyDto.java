package pl.chrono.timemanager.dto.response;

import java.time.DayOfWeek;
import java.util.Set;

public record PolicyDto(
    Integer id,
    String name,
    Integer dailyHours,
    Integer maxDailyHours,
    Integer maxWeeklyHours,
    Integer roundingMinutes,
    Set<DayOfWeek> workingDays
) {
}
