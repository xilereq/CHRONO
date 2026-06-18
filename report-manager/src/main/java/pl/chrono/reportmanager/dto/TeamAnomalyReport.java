package pl.chrono.reportmanager.dto;

import java.time.LocalDate;
import java.util.List;

public record TeamAnomalyReport(
    Integer managerId,
    LocalDate startDate,
    LocalDate endDate,
    List<TeamAnomalyEntry> entries
) {}
