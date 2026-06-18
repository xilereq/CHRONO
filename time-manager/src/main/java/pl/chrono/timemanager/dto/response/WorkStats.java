package pl.chrono.timemanager.dto.response;


public record WorkStats(
    int totalWorkTime,
    int remainingWorkTime,
    int totalMonthlyHours,
    int overtimeSeconds
) {
}
