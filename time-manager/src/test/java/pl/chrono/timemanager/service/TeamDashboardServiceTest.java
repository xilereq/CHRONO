package pl.chrono.timemanager.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
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
  private static final LocalDate DATE = LocalDate.now();

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
    when(timeData.findWorkLogsByUsersAndPeriod(any(), any(), any())).thenReturn(List.of(
        workLog(office, WorkLocationType.OFFICE, DATE.atTime(8, 0), null),
        workLog(home, WorkLocationType.HOME_OFFICE, DATE.atTime(9, 0), DATE.atTime(17, 0))));
    when(applicationData.findApprovedApplicationsByEmployeesAndDate(any(), eq(DATE))).thenReturn(List.of(
        application(leave, ApplicationType.HOLIDAY_LEAVE),
        application(plannedHome, ApplicationType.HOME_OFFICE)));

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
  void queriesWholeTeamInSingleBatchPerSource() {
    User a = employee(1, "Anna", "A");
    User b = employee(2, "Bartek", "B");
    User c = employee(3, "Celina", "C");
    when(userData.getSubordinatesForManager(MANAGER_ID)).thenReturn(List.of(a, b, c));

    service.getTeamDashboard(MANAGER_ID, null, DATE);

    verify(timeData, times(1)).findWorkLogsByUsersAndPeriod(
        eq(Set.of(1, 2, 3)), eq(DATE.minusDays(1).atStartOfDay()), any());
    verify(applicationData, times(1)).findApprovedApplicationsByEmployeesAndDate(Set.of(1, 2, 3), DATE);
    verify(timeData, never()).findWorkLogsByUserAndDate(anyInt(), any(), any());
    verify(applicationData, never()).findApprovedApplicationsByEmployeeAndDate(anyInt(), any());
  }

  @Test
  void emptyTeamSkipsPresenceQueries() {
    when(userData.getSubordinatesForManager(MANAGER_ID)).thenReturn(List.of());

    TeamDashboardDto result = service.getTeamDashboard(MANAGER_ID, null, DATE);

    assertThat(result.members()).isEmpty();
    assertThat(result.summary().total()).isZero();
    verifyNoInteractions(timeData, applicationData);
  }

  @Test
  void activeWorkLogLocationWinsOverEarlierOne() {
    User employee = employee(1, "Anna", "Zmiana");
    when(userData.getSubordinatesForManager(MANAGER_ID)).thenReturn(List.of(employee));
    when(timeData.findWorkLogsByUsersAndPeriod(any(), any(), any())).thenReturn(List.of(
        workLog(employee, WorkLocationType.OFFICE, DATE.atTime(8, 0), DATE.atTime(12, 0)),
        workLog(employee, WorkLocationType.HOME_OFFICE, DATE.atTime(13, 0), null)));

    TeamMemberStatusDto status = service.getTeamDashboard(MANAGER_ID, null, DATE).members().get(0);

    assertThat(status.status()).isEqualTo(TeamPresenceStatus.HOME_OFFICE);
    assertThat(status.currentlyWorking()).isTrue();
    assertThat(status.firstStartTime()).isEqualTo(DATE.atTime(8, 0));
  }

  @Test
  void overnightShiftFromPreviousDayCountsAsPresent() {
    User nightShift = employee(1, "Nocna", "Zmiana");
    when(userData.getSubordinatesForManager(MANAGER_ID)).thenReturn(List.of(nightShift));
    LocalDateTime yesterday22 = DATE.minusDays(1).atTime(22, 0);
    when(timeData.findWorkLogsByUsersAndPeriod(any(), any(), any())).thenReturn(List.of(
        workLog(nightShift, WorkLocationType.OFFICE, yesterday22, DATE.atTime(6, 0))));

    TeamMemberStatusDto status = service.getTeamDashboard(MANAGER_ID, null, DATE).members().get(0);

    assertThat(status.status()).isEqualTo(TeamPresenceStatus.OFFICE);
    assertThat(status.firstStartTime()).isEqualTo(yesterday22);
  }

  @Test
  void workLogClosedBeforeMidnightIsIgnored() {
    User employee = employee(1, "Anna", "Wczoraj");
    when(userData.getSubordinatesForManager(MANAGER_ID)).thenReturn(List.of(employee));
    when(timeData.findWorkLogsByUsersAndPeriod(any(), any(), any())).thenReturn(List.of(
        workLog(employee, WorkLocationType.OFFICE,
            DATE.minusDays(1).atTime(8, 0), DATE.minusDays(1).atTime(16, 0))));

    TeamMemberStatusDto status = service.getTeamDashboard(MANAGER_ID, null, DATE).members().get(0);

    assertThat(status.status()).isEqualTo(TeamPresenceStatus.NOT_CHECKED_IN);
  }

  @Test
  void skipsEmployeesNotEmployedOnDate() {
    User former = employee(1, "Anna", "Byla");
    former.setEmploymentEndDate(DATE.minusDays(1));
    when(userData.getSubordinatesForManager(MANAGER_ID)).thenReturn(List.of(former));

    TeamDashboardDto result = service.getTeamDashboard(MANAGER_ID, null, DATE);

    assertThat(result.members()).isEmpty();
    verifyNoInteractions(timeData, applicationData);
  }

  @Test
  void rejectsNonPositiveManagerId() {
    assertThatThrownBy(() -> service.getTeamDashboard(MANAGER_ID, 0, DATE))
        .isInstanceOf(TeamDashboardException.InvalidManagerId.class);
    assertThatThrownBy(() -> service.getTeamDashboard(MANAGER_ID, -5, DATE))
        .isInstanceOf(TeamDashboardException.InvalidManagerId.class);
    verifyNoInteractions(userData);
  }

  @Test
  void rejectsDateTooFarInPast() {
    LocalDate tooEarly = LocalDate.now().minusDays(TeamDashboardService.MAX_DAYS_FROM_TODAY + 1);

    assertThatThrownBy(() -> service.getTeamDashboard(MANAGER_ID, null, tooEarly))
        .isInstanceOf(TeamDashboardException.DateOutOfRange.class);
    verifyNoInteractions(userData);
  }

  @Test
  void rejectsDateTooFarInFuture() {
    LocalDate tooLate = LocalDate.now().plusDays(TeamDashboardService.MAX_DAYS_FROM_TODAY + 1);

    assertThatThrownBy(() -> service.getTeamDashboard(MANAGER_ID, null, tooLate))
        .isInstanceOf(TeamDashboardException.DateOutOfRange.class);
  }

  @Test
  void acceptsDateOnRangeBoundary() {
    LocalDate boundary = LocalDate.now().plusDays(TeamDashboardService.MAX_DAYS_FROM_TODAY);
    when(userData.getSubordinatesForManager(MANAGER_ID)).thenReturn(List.of());

    assertThat(service.getTeamDashboard(MANAGER_ID, null, boundary).date()).isEqualTo(boundary);
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

  private static WorkLog workLog(User user, WorkLocationType location, LocalDateTime start,
                                 LocalDateTime end) {
    WorkLog log = new WorkLog();
    log.setUser(user);
    log.setLocationType(location);
    log.setStartTime(start);
    log.setEndTime(end);
    return log;
  }

  private static Application application(User employee, ApplicationType type) {
    Application app = new Application();
    app.setEmployee(employee);
    app.setType(type);
    return app;
  }
}
