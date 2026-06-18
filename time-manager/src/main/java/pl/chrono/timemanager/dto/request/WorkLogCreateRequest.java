package pl.chrono.timemanager.dto.request;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record WorkLogCreateRequest(
    LocalDate date,
    LocalDateTime proposedStartTime,
    LocalDateTime proposedEndTime,
    String reason
) {
}

