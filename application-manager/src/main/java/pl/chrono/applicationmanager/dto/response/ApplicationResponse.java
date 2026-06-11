package pl.chrono.applicationmanager.dto.response;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record ApplicationResponse(
    Long id,
    String type,
    String status,
    LocalDate startDate,
    LocalDate endDate,
    Integer durationSeconds,
    String reason,
    LocalDateTime creationDateTime,
    LocalDateTime decisionDateTime,
    String decisionReason,
    Integer employeeId,
    String employeeEmail,
    Integer reviewerId,
    String reviewerEmail
) {}
