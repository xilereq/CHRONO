package pl.chrono.reportmanager.dto;
import java.time.LocalDate;
import java.util.List;

public record TeamOvertimeReport(
    Integer managerId,
    LocalDate startDate,
    LocalDate endDate,
    List<TeamOvertimeEntry> entries
) {}
