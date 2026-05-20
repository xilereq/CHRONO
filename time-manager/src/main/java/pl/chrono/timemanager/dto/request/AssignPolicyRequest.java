package pl.chrono.timemanager.dto.request;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record AssignPolicyRequest(
    @NotNull(message = "Policy ID is required")
    Integer policyId,

    @NotNull(message = "Start date is required")
    LocalDate startDate
) {}
