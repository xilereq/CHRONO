package pl.chrono.reportmanager.dto;

import java.time.LocalDate;
import java.util.List;

public record TeamMatrixReport(
    Integer managerId,
    LocalDate startDate,
    LocalDate endDate,
    List<String> employeeNames,
    List<TeamMatrixDay> days
) {}
