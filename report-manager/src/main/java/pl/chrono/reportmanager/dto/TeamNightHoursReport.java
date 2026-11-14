package pl.chrono.reportmanager.dto;

import java.time.LocalDate;
import java.util.List;

public record TeamNightHoursReport(
    Integer managerId,
    LocalDate startDate,
    LocalDate endDate,
    List<TeamNightHoursEntry> entries
) {}
