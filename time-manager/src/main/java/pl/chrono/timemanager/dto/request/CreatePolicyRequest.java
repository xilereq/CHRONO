package pl.chrono.timemanager.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.util.List;

public record CreatePolicyRequest(
    @NotBlank
    String name,

    @NotNull @Positive
    Integer dailyHours,

    @NotNull @Positive
    Integer maxDailyHours,

    @NotNull @Positive
    Integer maxWeeklyHours,

    List<String> workingDays
) {}
