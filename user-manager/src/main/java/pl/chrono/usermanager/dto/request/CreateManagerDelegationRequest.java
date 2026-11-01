package pl.chrono.usermanager.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record CreateManagerDelegationRequest(
    Integer managerId,
    @NotNull Integer deputyId,
    @NotNull LocalDate startDate,
    @NotNull LocalDate endDate,
    @Size(max = 255) String reason
) {}
