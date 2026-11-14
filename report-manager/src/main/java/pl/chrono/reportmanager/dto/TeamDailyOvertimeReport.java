package pl.chrono.reportmanager.dto;

import java.time.LocalDate;
import java.util.List;

public record TeamDailyOvertimeReport(
    Integer managerId,
    LocalDate startDate,
    LocalDate endDate,
    List<TeamDailyOvertimeEntry> entries
) {}
