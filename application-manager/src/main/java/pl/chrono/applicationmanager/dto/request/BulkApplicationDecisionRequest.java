package pl.chrono.applicationmanager.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public record BulkApplicationDecisionRequest(
    @NotEmpty
    @Size(max = 100)
    List<@NotNull Long> applicationIds,
    String decisionReason
) {}
