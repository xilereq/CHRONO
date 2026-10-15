package pl.chrono.timemanager.dto.response;

import java.time.LocalDate;

public record HolidayDto(
    Integer holidayId,
    String name,
    LocalDate date
) {
}
