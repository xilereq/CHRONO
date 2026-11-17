package pl.chrono.workforceapi.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pl.chrono.usermanager.api.I_TeamHierarchyService;
import pl.chrono.usermanager.dto.response.TeamTreeNodeDto;
import pl.chrono.usermanager.service.AuthService;

@RestController
@RequestMapping("/api/users/team-tree")
public class TeamHierarchyController extends BaseController {

  private final I_TeamHierarchyService teamHierarchyService;

  public TeamHierarchyController(AuthService authService,
                                 I_TeamHierarchyService teamHierarchyService) {
    super(authService);
    this.teamHierarchyService = teamHierarchyService;
  }

  /**
   * Subordination tree rooted at {@code managerId} (defaults to the requester).
   * Superiors in the chain of command, active deputies and HR/Admin may view other branches.
   */
  @GetMapping
  @PreAuthorize("hasAnyRole('EMPLOYEE', 'MANAGER', 'HR', 'ADMIN')")
  public ResponseEntity<TeamTreeNodeDto> getTeamTree(
      @RequestHeader("Authorization") String token,
      @RequestParam(value = "managerId", required = false) Integer managerId,
      @RequestParam(value = "depth", required = false) Integer depth,
      @RequestParam(value = "includeInactive", defaultValue = "false") boolean includeInactive) {

    return ResponseEntity.ok(
        teamHierarchyService.getTeamTree(extractId(token), managerId, depth, includeInactive));
  }
}
