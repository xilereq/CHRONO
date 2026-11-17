package pl.chrono.timemanager.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.chrono.dbmanager.model.Application;
import pl.chrono.dbmanager.model.User;
import pl.chrono.dbmanager.model.WorkLog;
import pl.chrono.dbmanager.model.enums.ApplicationType;
import pl.chrono.dbmanager.model.enums.WorkLocationType;
import pl.chrono.dbmanager.ports.I_ApplicationData;
import pl.chrono.dbmanager.ports.I_TimeData;
import pl.chrono.dbmanager.ports.I_UserData;
import pl.chrono.timemanager.api.I_TeamDashboardService;
import pl.chrono.timemanager.dto.response.TeamDashboardDto;
import pl.chrono.timemanager.dto.response.TeamDashboardSummary;
import pl.chrono.timemanager.dto.response.TeamMemberStatusDto;
import pl.chrono.timemanager.dto.response.TeamPresenceStatus;
import pl.chrono.timemanager.exception.TeamDashboardException;
import pl.chrono.timemanager.exception.UserException;
import pl.chrono.usermanager.api.I_ManagerDelegationService;

@Service
@RequiredArgsConstructor
public class TeamDashboardService implements I_TeamDashboardService {

  private static final Set<String> PRIVILEGED_ROLES = Set.of("ROLE_ADMIN", "ROLE_HR");
  private static final Set<ApplicationType> LEAVE_TYPES =
      EnumSet.of(ApplicationType.HOLIDAY_LEAVE, ApplicationType.SICK_LEAVE);

  private final I_UserData userData;
  private final I_TimeData timeData;
  private final I_ApplicationData applicationData;
  private final I_ManagerDelegationService delegationService;

  @Transactional(readOnly = true)
  @Override
  public TeamDashboardDto getTeamDashboard(Integer requesterId, Integer managerId, LocalDate date) {
    Integer targetManagerId = managerId != null ? managerId : requesterId;
    LocalDate targetDate = date != null ? date : LocalDate.now();

    validateAccess(requesterId, targetManagerId);

    List<TeamMemberStatusDto> members = userData.getSubordinatesForManager(targetManagerId).stream()
        .filter(employee -> employee.isEmployedOn(targetDate))
        .map(employee -> resolveStatus(employee, targetDate))
        .sorted(Comparator.comparing(TeamMemberStatusDto::fullName, String.CASE_INSENSITIVE_ORDER))
        .toList();

    return new TeamDashboardDto(targetManagerId, targetDate, summarize(members), members);
  }

  private void validateAccess(Integer requesterId, Integer managerId) {
    if (requesterId.equals(managerId)) {
      return;
    }
    userData.findById(managerId).orElseThrow(() -> new UserException.NotFound(managerId));

    if (delegationService.isActiveDeputyOf(requesterId, managerId)) {
      return;
    }
    User requester = userData.findById(requesterId)
        .orElseThrow(() -> new UserException.NotFound(requesterId));
    boolean privileged = requester.getRoles().stream()
        .anyMatch(role -> PRIVILEGED_ROLES.contains(role.getName()));
    if (!privileged) {
      throw new TeamDashboardException.AccessDenied(managerId);
    }
  }

  private TeamMemberStatusDto resolveStatus(User employee, LocalDate date) {
    Integer employeeId = employee.getUserId();
    String fullName = employee.getName() + " " + employee.getSurname();

    LocalDateTime startOfDay = date.atStartOfDay();
    LocalDateTime endOfDay = date.plusDays(1).atStartOfDay().minusNanos(1);
    List<WorkLog> workLogs = timeData.findWorkLogsByUserAndDate(employeeId, startOfDay, endOfDay);

    // Actual registered work has priority over what was planned in applications.
    if (!workLogs.isEmpty()) {
      Optional<WorkLog> active = workLogs.stream().filter(log -> log.getEndTime() == null).findFirst();
      WorkLog current = active.orElse(workLogs.get(workLogs.size() - 1));
      TeamPresenceStatus status = current.getLocationType() == WorkLocationType.HOME_OFFICE
          ? TeamPresenceStatus.HOME_OFFICE
          : TeamPresenceStatus.OFFICE;
      return new TeamMemberStatusDto(employeeId, fullName, status, active.isPresent(),
          workLogs.get(0).getStartTime(), null);
    }

    List<Application> approved = applicationData.findApprovedApplicationsByEmployeeAndDate(employeeId, date);

    Optional<ApplicationType> leaveType = approved.stream()
        .map(Application::getType)
        .filter(LEAVE_TYPES::contains)
        .findFirst();
    if (leaveType.isPresent()) {
      return new TeamMemberStatusDto(employeeId, fullName, TeamPresenceStatus.LEAVE, false, null,
          leaveType.get());
    }

    boolean plannedHomeOffice = approved.stream()
        .anyMatch(app -> app.getType() == ApplicationType.HOME_OFFICE);
    TeamPresenceStatus status = plannedHomeOffice
        ? TeamPresenceStatus.HOME_OFFICE
        : TeamPresenceStatus.NOT_CHECKED_IN;
    return new TeamMemberStatusDto(employeeId, fullName, status, false, null, null);
  }

  private TeamDashboardSummary summarize(List<TeamMemberStatusDto> members) {
    return new TeamDashboardSummary(
        members.size(),
        count(members, TeamPresenceStatus.OFFICE),
        count(members, TeamPresenceStatus.HOME_OFFICE),
        count(members, TeamPresenceStatus.LEAVE),
        count(members, TeamPresenceStatus.NOT_CHECKED_IN)
    );
  }

  private int count(List<TeamMemberStatusDto> members, TeamPresenceStatus status) {
    return (int) members.stream().filter(member -> member.status() == status).count();
  }
}
