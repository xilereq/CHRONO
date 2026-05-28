package pl.chrono.reportmanager.dto;

import java.time.LocalDate;
import java.util.List;

public record TeamAbsencesReport(
    Integer managerId,
    LocalDate startDate,
    LocalDate endDate,
    List<TeamAbsenceEntry> entries
) {}
