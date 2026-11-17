package pl.chrono.workforceapi.controller;

import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pl.chrono.timemanager.api.I_TeamDashboardService;
import pl.chrono.timemanager.dto.response.TeamDashboardDto;
import pl.chrono.usermanager.service.AuthService;

@RestController
@RequestMapping("/api/team-dashboard")
public class TeamDashboardController extends BaseController {

  private final I_TeamDashboardService teamDashboardService;

  public TeamDashboardController(AuthService authService,
                                 I_TeamDashboardService teamDashboardService) {
    super(authService);
    this.teamDashboardService = teamDashboardService;
  }

  /**
   * Team presence (office / home office / leave) for the given day.
   * Deputies can pass {@code managerId} of the manager they substitute; HR/Admin can pass any.
   */
  @GetMapping
  @PreAuthorize("hasAnyRole('EMPLOYEE', 'MANAGER', 'HR', 'ADMIN')")
  public ResponseEntity<TeamDashboardDto> getTeamDashboard(
      @RequestHeader("Authorization") String token,
      @RequestParam(value = "date", required = false)
      @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
      @RequestParam(value = "managerId", required = false) Integer managerId) {

    return ResponseEntity.ok(teamDashboardService.getTeamDashboard(extractId(token), managerId, date));
  }
}
