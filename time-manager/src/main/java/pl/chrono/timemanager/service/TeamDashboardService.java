package pl.chrono.timemanager.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
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

  static final int MAX_DAYS_FROM_TODAY = 365;

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

    validateInput(targetManagerId, targetDate);
    validateAccess(requesterId, targetManagerId);

    List<User> team = userData.getSubordinatesForManager(targetManagerId).stream()
        .filter(employee -> employee.isEmployedOn(targetDate))
        .toList();
    if (team.isEmpty()) {
      return new TeamDashboardDto(targetManagerId, targetDate, summarize(List.of()), List.of());
    }

    // Two batched queries for the whole team instead of two queries per employee.
    Set<Integer> teamIds = team.stream().map(User::getUserId).collect(Collectors.toSet());
    Map<Integer, List<WorkLog>> workLogsByUser = loadWorkLogsOverlappingDay(teamIds, targetDate);
    Map<Integer, List<Application>> applicationsByUser = applicationData
        .findApprovedApplicationsByEmployeesAndDate(teamIds, targetDate).stream()
        .collect(Collectors.groupingBy(app -> app.getEmployee().getUserId()));

    List<TeamMemberStatusDto> members = team.stream()
        .map(employee -> resolveStatus(
            employee,
            workLogsByUser.getOrDefault(employee.getUserId(), List.of()),
            applicationsByUser.getOrDefault(employee.getUserId(), List.of())))
        .sorted(Comparator.comparing(TeamMemberStatusDto::fullName, String.CASE_INSENSITIVE_ORDER))
        .toList();

    return new TeamDashboardDto(targetManagerId, targetDate, summarize(members), members);
  }

  private void validateInput(Integer managerId, LocalDate date) {
    if (managerId == null || managerId <= 0) {
      throw new TeamDashboardException.InvalidManagerId(managerId);
    }
    LocalDate today = LocalDate.now();
    LocalDate min = today.minusDays(MAX_DAYS_FROM_TODAY);
    LocalDate max = today.plusDays(MAX_DAYS_FROM_TODAY);
    if (date.isBefore(min) || date.isAfter(max)) {
      throw new TeamDashboardException.DateOutOfRange(date, min, max);
    }
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

  /**
   * Loads logs that overlap the given day, including overnight sessions started the day before
   * that were still running at (or after) midnight.
   */
  private Map<Integer, List<WorkLog>> loadWorkLogsOverlappingDay(Set<Integer> userIds, LocalDate date) {
    LocalDateTime startOfDay = date.atStartOfDay();
    LocalDateTime endOfDay = date.plusDays(1).atStartOfDay().minusNanos(1);

    return timeData.findWorkLogsByUsersAndPeriod(userIds, startOfDay.minusDays(1), endOfDay).stream()
        .filter(log -> !log.getStartTime().isBefore(startOfDay)
            || log.getEndTime() == null
            || !log.getEndTime().isBefore(startOfDay))
        .collect(Collectors.groupingBy(log -> log.getUser().getUserId()));
  }

  private TeamMemberStatusDto resolveStatus(User employee, List<WorkLog> workLogs,
                                            List<Application> approvedApplications) {
    Integer employeeId = employee.getUserId();
    String fullName = employee.getName() + " " + employee.getSurname();

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

    Optional<ApplicationType> leaveType = approvedApplications.stream()
        .map(Application::getType)
        .filter(LEAVE_TYPES::contains)
        .findFirst();
    if (leaveType.isPresent()) {
      return new TeamMemberStatusDto(employeeId, fullName, TeamPresenceStatus.LEAVE, false, null,
          leaveType.get());
    }

    boolean plannedHomeOffice = approvedApplications.stream()
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
