package pl.chrono.timemanager.dto.response;

import java.time.LocalDate;
import java.util.List;

public record TeamDashboardDto(
    Integer managerId,
    LocalDate date,
    TeamDashboardSummary summary,
    List<TeamMemberStatusDto> members
) {
}
