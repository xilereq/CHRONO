package pl.chrono.timemanager.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pl.chrono.dbmanager.model.Application;
import pl.chrono.dbmanager.model.Role;
import pl.chrono.dbmanager.model.User;
import pl.chrono.dbmanager.model.WorkLog;
import pl.chrono.dbmanager.model.enums.ApplicationType;
import pl.chrono.dbmanager.model.enums.WorkLocationType;
import pl.chrono.dbmanager.ports.I_ApplicationData;
import pl.chrono.dbmanager.ports.I_TimeData;
import pl.chrono.dbmanager.ports.I_UserData;
import pl.chrono.timemanager.dto.response.TeamDashboardDto;
import pl.chrono.timemanager.dto.response.TeamMemberStatusDto;
import pl.chrono.timemanager.dto.response.TeamPresenceStatus;
import pl.chrono.timemanager.exception.TeamDashboardException;
import pl.chrono.timemanager.exception.UserException;
import pl.chrono.usermanager.api.I_ManagerDelegationService;

@ExtendWith(MockitoExtension.class)
class TeamDashboardServiceTest {

  private static final Integer MANAGER_ID = 10;
  private static final Integer OTHER_ID = 99;
  private static final LocalDate DATE = LocalDate.of(2026, 11, 17);

  @Mock
  private I_UserData userData;
  @Mock
  private I_TimeData timeData;
  @Mock
  private I_ApplicationData applicationData;
  @Mock
  private I_ManagerDelegationService delegationService;

  @InjectMocks
  private TeamDashboardService service;

  @Test
  void aggregatesOfficeHomeLeaveAndMissingStatuses() {
    User office = employee(1, "Anna", "Biuro");
    User home = employee(2, "Bartek", "Dom");
    User leave = employee(3, "Celina", "Urlop");
    User plannedHome = employee(4, "Darek", "Plan");
    User missing = employee(5, "Ewa", "Brak");
    when(userData.getSubordinatesForManager(MANAGER_ID))
        .thenReturn(List.of(office, home, leave, plannedHome, missing));

    when(timeData.findWorkLogsByUserAndDate(eq(1), any(), any()))
        .thenReturn(List.of(workLog(office, WorkLocationType.OFFICE, 8, null)));
    when(timeData.findWorkLogsByUserAndDate(eq(2), any(), any()))
        .thenReturn(List.of(workLog(home, WorkLocationType.HOME_OFFICE, 9, 17)));
    when(applicationData.findApprovedApplicationsByEmployeeAndDate(3, DATE))
        .thenReturn(List.of(application(ApplicationType.HOLIDAY_LEAVE)));
    when(applicationData.findApprovedApplicationsByEmployeeAndDate(4, DATE))
        .thenReturn(List.of(application(ApplicationType.HOME_OFFICE)));

    TeamDashboardDto result = service.getTeamDashboard(MANAGER_ID, null, DATE);

    assertThat(result.managerId()).isEqualTo(MANAGER_ID);
    assertThat(result.date()).isEqualTo(DATE);
    assertThat(result.members()).extracting(TeamMemberStatusDto::status).containsExactly(
        TeamPresenceStatus.OFFICE,
        TeamPresenceStatus.HOME_OFFICE,
        TeamPresenceStatus.LEAVE,
        TeamPresenceStatus.HOME_OFFICE,
        TeamPresenceStatus.NOT_CHECKED_IN);

    TeamMemberStatusDto officeStatus = result.members().get(0);
    assertThat(officeStatus.currentlyWorking()).isTrue();
    assertThat(officeStatus.firstStartTime()).isEqualTo(DATE.atTime(8, 0));
    assertThat(result.members().get(1).currentlyWorking()).isFalse();
    assertThat(result.members().get(2).leaveType()).isEqualTo(ApplicationType.HOLIDAY_LEAVE);

    assertThat(result.summary().total()).isEqualTo(5);
    assertThat(result.summary().office()).isEqualTo(1);
    assertThat(result.summary().homeOffice()).isEqualTo(2);
    assertThat(result.summary().onLeave()).isEqualTo(1);
    assertThat(result.summary().notCheckedIn()).isEqualTo(1);
  }

  @Test
  void activeWorkLogLocationWinsOverEarlierOne() {
    User employee = employee(1, "Anna", "Zmiana");
    when(userData.getSubordinatesForManager(MANAGER_ID)).thenReturn(List.of(employee));
    when(timeData.findWorkLogsByUserAndDate(eq(1), any(), any())).thenReturn(List.of(
        workLog(employee, WorkLocationType.OFFICE, 8, 12),
        workLog(employee, WorkLocationType.HOME_OFFICE, 13, null)));

    TeamDashboardDto result = service.getTeamDashboard(MANAGER_ID, null, DATE);

    TeamMemberStatusDto status = result.members().get(0);
    assertThat(status.status()).isEqualTo(TeamPresenceStatus.HOME_OFFICE);
    assertThat(status.currentlyWorking()).isTrue();
    assertThat(status.firstStartTime()).isEqualTo(DATE.atTime(8, 0));
  }

  @Test
  void skipsEmployeesNotEmployedOnDate() {
    User former = employee(1, "Anna", "Byla");
    former.setEmploymentEndDate(DATE.minusDays(1));
    when(userData.getSubordinatesForManager(MANAGER_ID)).thenReturn(List.of(former));

    TeamDashboardDto result = service.getTeamDashboard(MANAGER_ID, null, DATE);

    assertThat(result.members()).isEmpty();
    assertThat(result.summary().total()).isZero();
  }

  @Test
  void deputyCanViewSubstitutedManagersTeam() {
    when(userData.findById(MANAGER_ID)).thenReturn(Optional.of(user(MANAGER_ID, "ROLE_MANAGER")));
    when(delegationService.isActiveDeputyOf(OTHER_ID, MANAGER_ID)).thenReturn(true);
    when(userData.getSubordinatesForManager(MANAGER_ID)).thenReturn(List.of());

    TeamDashboardDto result = service.getTeamDashboard(OTHER_ID, MANAGER_ID, DATE);

    assertThat(result.managerId()).isEqualTo(MANAGER_ID);
    verify(userData, never()).findById(OTHER_ID);
  }

  @Test
  void hrCanViewAnyTeam() {
    when(userData.findById(MANAGER_ID)).thenReturn(Optional.of(user(MANAGER_ID, "ROLE_MANAGER")));
    when(userData.findById(OTHER_ID)).thenReturn(Optional.of(user(OTHER_ID, "ROLE_HR")));
    when(userData.getSubordinatesForManager(MANAGER_ID)).thenReturn(List.of());

    assertThat(service.getTeamDashboard(OTHER_ID, MANAGER_ID, DATE).managerId()).isEqualTo(MANAGER_ID);
  }

  @Test
  void strangerIsDenied() {
    when(userData.findById(MANAGER_ID)).thenReturn(Optional.of(user(MANAGER_ID, "ROLE_MANAGER")));
    when(userData.findById(OTHER_ID)).thenReturn(Optional.of(user(OTHER_ID, "ROLE_EMPLOYEE")));

    assertThatThrownBy(() -> service.getTeamDashboard(OTHER_ID, MANAGER_ID, DATE))
        .isInstanceOf(TeamDashboardException.AccessDenied.class);
    verify(userData, never()).getSubordinatesForManager(any());
  }

  @Test
  void unknownManagerThrowsNotFound() {
    when(userData.findById(MANAGER_ID)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.getTeamDashboard(OTHER_ID, MANAGER_ID, DATE))
        .isInstanceOf(UserException.NotFound.class);
  }

  private static User employee(int id, String name, String surname) {
    User user = new User();
    user.setUserId(id);
    user.setName(name);
    user.setSurname(surname);
    return user;
  }

  private static User user(int id, String roleName) {
    User user = employee(id, "U", String.valueOf(id));
    Role role = new Role();
    role.setName(roleName);
    user.setRoles(Set.of(role));
    return user;
  }

  private static WorkLog workLog(User user, WorkLocationType location, int startHour, Integer endHour) {
    WorkLog log = new WorkLog();
    log.setUser(user);
    log.setLocationType(location);
    log.setStartTime(DATE.atTime(startHour, 0));
    log.setEndTime(endHour != null ? DATE.atTime(endHour, 0) : null);
    return log;
  }

  private static Application application(ApplicationType type) {
    Application app = new Application();
    app.setType(type);
    return app;
  }
}
