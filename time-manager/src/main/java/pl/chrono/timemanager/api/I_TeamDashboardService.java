package pl.chrono.timemanager.api;

import java.time.LocalDate;
import pl.chrono.timemanager.dto.response.TeamDashboardDto;

public interface I_TeamDashboardService {

  /**
   * Aggregates the presence of a manager's team on a given day (office / home office / leave).
   *
   * @param requesterId the authenticated user
   * @param managerId   the manager whose team is shown; {@code null} means the requester's own team
   * @param date        the day to inspect; {@code null} means today
   */
  TeamDashboardDto getTeamDashboard(Integer requesterId, Integer managerId, LocalDate date);
}
