package pl.chrono.reportmanager.dto;

import java.util.List;

public record TeamWorkloadReport(
    Integer managerId,
    int year,
    int month,
    List<TeamWorkloadEntry> entries
) {}

