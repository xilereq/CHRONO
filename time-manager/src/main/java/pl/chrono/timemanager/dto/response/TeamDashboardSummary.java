package pl.chrono.timemanager.dto.response;

public record TeamDashboardSummary(
    int total,
    int office,
    int homeOffice,
    int onLeave,
    int notCheckedIn
) {
}
