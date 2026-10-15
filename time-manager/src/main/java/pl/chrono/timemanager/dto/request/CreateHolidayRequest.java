package pl.chrono.timemanager.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record CreateHolidayRequest(
    @NotBlank
    String name,

    @NotNull
    LocalDate date
) {
}
