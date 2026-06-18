package pl.chrono.timemanager.dto.request;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record MassAssignHolidayRequest(
    @NotEmpty(message = "Holiday list cannot be empty")
    List<Integer> holidayIds
) {
}
