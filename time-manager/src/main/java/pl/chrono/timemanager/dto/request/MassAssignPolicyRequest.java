package pl.chrono.timemanager.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.List;

public record MassAssignPolicyRequest(
    @NotNull(message = "Policy ID is requiered")
    Integer policyId,

    @NotEmpty(message = "User IDs list cannot be empty")
    List<Integer> userIds,

    @NotNull(message = "Start date is required")
    LocalDate startDate
) {}
