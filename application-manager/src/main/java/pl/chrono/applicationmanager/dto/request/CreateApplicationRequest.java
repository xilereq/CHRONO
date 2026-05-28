package pl.chrono.applicationmanager.dto.request;

import java.time.LocalDate;

public record CreateApplicationRequest(
    String type,
    LocalDate startDate,
    LocalDate endDate,
    String reason,
    Integer durationSeconds
) {}
