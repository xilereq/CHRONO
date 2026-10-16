package pl.chrono.timemanager.dto.response;

import java.time.LocalDate;

public record ExternalHolidayDto(
    String name,
    String originalName,
    LocalDate date,
    boolean alreadyExists
) {
}
