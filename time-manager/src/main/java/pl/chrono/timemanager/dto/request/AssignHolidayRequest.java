package pl.chrono.timemanager.dto.request;

import jakarta.validation.constraints.NotNull;

public record AssignHolidayRequest(
    @NotNull()
    Integer holidayId
) {
}
