package pl.chrono.reportmanager.dto;
import java.time.LocalDate;

public record OvertimeEntry(
    LocalDate date,
    Long workedSeconds,
    Integer requiredSeconds,
    Long overtimeSeconds
) {}
