package pl.chrono.reportmanager.dto;
import java.time.LocalDate;
import java.util.List;

public record EmployeeOvertimeReport(
    Integer employeeId,
    String employeeFullName,
    LocalDate startDate,
    LocalDate endDate,
    Long totalOvertimeSeconds,
    List<OvertimeEntry> entries
) {}
