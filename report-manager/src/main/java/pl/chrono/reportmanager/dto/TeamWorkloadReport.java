package pl.chrono.reportmanager.dto;

import java.time.LocalDate;
import java.util.List;

public record TeamWorkloadReport(
    Integer managerId,
    LocalDate startDate,
    LocalDate endDate,
    List<TeamWorkloadEntry> entries
) {}

