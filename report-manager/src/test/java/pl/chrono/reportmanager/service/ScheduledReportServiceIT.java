package pl.chrono.reportmanager.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import jakarta.persistence.EntityManager;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;
import pl.chrono.dbmanager.model.Role;
import pl.chrono.dbmanager.model.ScheduledReport;
import pl.chrono.dbmanager.model.User;
import pl.chrono.dbmanager.model.enums.ReportPeriod;
import pl.chrono.dbmanager.model.enums.ScheduleFrequency;
import pl.chrono.dbmanager.model.enums.ScheduleRunStatus;
import pl.chrono.dbmanager.model.enums.ScheduledReportType;
import pl.chrono.dbmanager.repository.RoleRepository;
import pl.chrono.dbmanager.repository.ScheduledReportRepository;
import pl.chrono.dbmanager.repository.UserRepository;
import pl.chrono.reportmanager.TestConfig;
import pl.chrono.reportmanager.api.I_ReportFacade;
import pl.chrono.reportmanager.api.I_ScheduledReportService;
import pl.chrono.reportmanager.dto.ReportFormat;
import pl.chrono.reportmanager.dto.ScheduledReportRequestDto;
import pl.chrono.reportmanager.dto.ScheduledReportResponseDto;
import pl.chrono.reportmanager.exception.ReportScheduleException;
import pl.chrono.reportmanager.exception.ReportScheduleSecurityException;

@SpringBootTest(classes = TestConfig.class)
@Transactional
class ScheduledReportServiceIT {

  @Autowired
  private I_ScheduledReportService scheduledReportService;

  @Autowired
  private ScheduledReportRepository scheduledReportRepository;

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private RoleRepository roleRepository;

  @Autowired
  private EntityManager entityManager;

  @MockitoBean
  private I_ReportFacade reportFacade;

  private User employee;
  private User otherEmployee;
  private User manager;
  private User hrUser;

  @BeforeEach
  void setUp() {
    Role empRole = persistRole("ROLE_EMPLOYEE");
    Role mgrRole = persistRole("ROLE_MANAGER");
    Role hrRole = persistRole("ROLE_HR");

    manager = persistUser("Marek", "Manager", "marek.manager@chrono.pl", Set.of(mgrRole), null);
    employee = persistUser("Jan", "Kowalski", "jan.kowalski@chrono.pl", Set.of(empRole), manager);
    otherEmployee =
        persistUser("Piotr", "Nowak", "piotr.nowak@chrono.pl", Set.of(empRole), null);
    hrUser = persistUser("Anna", "Kadrowa", "anna.hr@chrono.pl", Set.of(hrRole), null);

    entityManager.flush();
  }

  @Test
  void shouldPerformFullLifecycleCreateGetUpdateToggleDelete() {
    ScheduledReportRequestDto createReq = new ScheduledReportRequestDto(
        "Mój raport dzienny",
        ScheduledReportType.EMPLOYEE_TIMESHEET,
        ReportFormat.PDF,
        ScheduleFrequency.DAILY,
        null,
        null,
        LocalTime.of(8, 0),
        ReportPeriod.PREVIOUS_DAY,
        null,
        null,
        List.of(employee.getUserId()),
        List.of("jan.kowalski@chrono.pl")
    );

    ScheduledReportResponseDto created =
        scheduledReportService.createSchedule(employee.getUserId(), createReq);

    assertThat(created.scheduleId()).isNotNull();
    assertThat(created.name()).isEqualTo("Mój raport dzienny");
    assertThat(created.creatorId()).isEqualTo(employee.getUserId());
    assertThat(created.creatorFullName()).isEqualTo("Jan Kowalski");
    assertThat(created.isActive()).isTrue();
    assertThat(created.targetUserIds()).containsExactly(employee.getUserId());
    assertThat(created.recipientEmails()).containsExactly("jan.kowalski@chrono.pl");

    ScheduledReportResponseDto fetched =
        scheduledReportService.getScheduleById(employee.getUserId(), created.scheduleId());
    assertThat(fetched.name()).isEqualTo("Mój raport dzienny");

    ScheduledReportRequestDto updateReq = new ScheduledReportRequestDto(
        "Mój raport tygodniowy",
        ScheduledReportType.EMPLOYEE_OVERTIME,
        ReportFormat.CSV,
        ScheduleFrequency.WEEKLY,
        1,
        null,
        LocalTime.of(9, 30),
        ReportPeriod.PREVIOUS_WEEK,
        null,
        null,
        List.of(employee.getUserId()),
        List.of("jan.kowalski@chrono.pl")
    );

    ScheduledReportResponseDto updated = scheduledReportService.updateSchedule(
        employee.getUserId(), created.scheduleId(), updateReq);
    assertThat(updated.name()).isEqualTo("Mój raport tygodniowy");
    assertThat(updated.reportType()).isEqualTo(ScheduledReportType.EMPLOYEE_OVERTIME);
    assertThat(updated.reportFormat()).isEqualTo(ReportFormat.CSV);
    assertThat(updated.frequency()).isEqualTo(ScheduleFrequency.WEEKLY);
    assertThat(updated.dayOfWeek()).isEqualTo(1);

    ScheduledReportResponseDto toggledOff =
        scheduledReportService.toggleSchedule(employee.getUserId(), created.scheduleId());
    assertThat(toggledOff.isActive()).isFalse();

    ScheduledReportResponseDto toggledOn =
        scheduledReportService.toggleSchedule(employee.getUserId(), created.scheduleId());
    assertThat(toggledOn.isActive()).isTrue();

    scheduledReportService.deleteSchedule(employee.getUserId(), created.scheduleId());
    entityManager.flush();

    assertThat(scheduledReportRepository.findById(created.scheduleId())).isEmpty();
    assertThatThrownBy(() ->
        scheduledReportService.getScheduleById(employee.getUserId(), created.scheduleId()))
        .isInstanceOf(ReportScheduleException.NotFound.class);
  }

  @Test
  void shouldEnforcePermissionsAcrossRoles() {
    ScheduledReportRequestDto empInvalidTeamReq = new ScheduledReportRequestDto(
        "Zespołowy",
        ScheduledReportType.TEAM_WORKLOAD,
        ReportFormat.PDF,
        ScheduleFrequency.DAILY,
        null,
        null,
        LocalTime.of(7, 0),
        ReportPeriod.PREVIOUS_DAY,
        null,
        null,
        List.of(employee.getUserId()),
        List.of(employee.getEmail())
    );

    assertThatThrownBy(() ->
        scheduledReportService.createSchedule(employee.getUserId(), empInvalidTeamReq))
        .isInstanceOf(ReportScheduleSecurityException.UnauthorizedTeamReport.class);

    ScheduledReportRequestDto mgrValidSubordinateReq = new ScheduledReportRequestDto(
        "Raport podwładnego",
        ScheduledReportType.TEAM_WORKLOAD,
        ReportFormat.XLSX,
        ScheduleFrequency.DAILY,
        null,
        null,
        LocalTime.of(7, 0),
        ReportPeriod.PREVIOUS_DAY,
        null,
        null,
        List.of(manager.getUserId(), employee.getUserId()),
        List.of(" " + manager.getEmail() + " ")
    );

    ScheduledReportResponseDto mgrCreated =
        scheduledReportService.createSchedule(manager.getUserId(), mgrValidSubordinateReq);
    assertThat(mgrCreated.scheduleId()).isNotNull();
    assertThat(mgrCreated.recipientEmails()).containsExactly(manager.getEmail());

    ScheduledReportRequestDto mgrInvalidTargetReq = new ScheduledReportRequestDto(
        "Raport obcego pracownika",
        ScheduledReportType.EMPLOYEE_TIMESHEET,
        ReportFormat.PDF,
        ScheduleFrequency.DAILY,
        null,
        null,
        LocalTime.of(7, 0),
        ReportPeriod.PREVIOUS_DAY,
        null,
        null,
        List.of(otherEmployee.getUserId()),
        List.of(manager.getEmail())
    );

    assertThatThrownBy(() ->
        scheduledReportService.createSchedule(manager.getUserId(), mgrInvalidTargetReq))
        .isInstanceOf(ReportScheduleSecurityException.UnauthorizedManagerTargetUsers.class);

    assertThatThrownBy(() ->
        scheduledReportService.getScheduleById(employee.getUserId(), mgrCreated.scheduleId()))
        .isInstanceOf(ReportScheduleSecurityException.UnauthorizedScheduleAccess.class);

    assertThat(scheduledReportService.getSchedules(employee.getUserId())).isEmpty();
    assertThat(scheduledReportService.getSchedules(manager.getUserId())).hasSize(1);
    assertThat(scheduledReportService.getSchedules(hrUser.getUserId())).hasSize(1);
  }

  @Test
  void shouldProcessDueSchedulesSuccessAndFailureWithRetries() {
    ScheduledReport dueSuccess = new ScheduledReport();
    dueSuccess.setName("Raport Sukces");
    dueSuccess.setCreator(employee);
    dueSuccess.setReportType(ScheduledReportType.EMPLOYEE_TIMESHEET);
    dueSuccess.setReportFormat("CSV");
    dueSuccess.setFrequency(ScheduleFrequency.DAILY);
    dueSuccess.setExecutionTime(LocalTime.of(7, 0));
    dueSuccess.setReportPeriod(ReportPeriod.PREVIOUS_DAY);
    dueSuccess.setRecipientEmails(employee.getEmail());
    dueSuccess.setActive(true);
    dueSuccess.setNextRunAt(LocalDateTime.now().minusMinutes(10));
    dueSuccess.setTargetUsers(new HashSet<>(Set.of(employee)));
    dueSuccess = scheduledReportRepository.save(dueSuccess);

    ScheduledReport dueFail = new ScheduledReport();
    dueFail.setName("Raport Błąd");
    dueFail.setCreator(manager);
    dueFail.setReportType(ScheduledReportType.TEAM_ANOMALY);
    dueFail.setReportFormat("PDF");
    dueFail.setFrequency(ScheduleFrequency.DAILY);
    dueFail.setExecutionTime(LocalTime.of(7, 0));
    dueFail.setReportPeriod(ReportPeriod.PREVIOUS_DAY);
    dueFail.setRecipientEmails(manager.getEmail());
    dueFail.setActive(true);
    LocalDateTime failNextRun = LocalDateTime.now().minusMinutes(5);
    dueFail.setNextRunAt(failNextRun);
    dueFail.setTargetUsers(new HashSet<>(Set.of(manager)));
    dueFail = scheduledReportRepository.save(dueFail);

    entityManager.flush();
    entityManager.clear();

    when(reportFacade.getEmployeeTimesheet(any(), any(), any(), any()))
        .thenReturn(new byte[]{1, 2, 3});
    when(reportFacade.getTeamAnomalyReport(any(), any(), any(), any()))
        .thenThrow(new RuntimeException("Błąd generowania PDF"));

    scheduledReportService.processDueSchedules();
    entityManager.flush();
    entityManager.clear();

    ScheduledReport updatedSuccess =
        scheduledReportRepository.findById(dueSuccess.getScheduleId()).orElseThrow();
    assertThat(updatedSuccess.getLastRunStatus()).isEqualTo(ScheduleRunStatus.SUCCESS);
    assertThat(updatedSuccess.getRetryCount()).isZero();
    assertThat(updatedSuccess.getNextRunAt()).isAfter(LocalDateTime.now());

    ScheduledReport updatedFail =
        scheduledReportRepository.findById(dueFail.getScheduleId()).orElseThrow();
    assertThat(updatedFail.getLastRunStatus()).isEqualTo(ScheduleRunStatus.FAILED);
    assertThat(updatedFail.getRetryCount()).isEqualTo(1);
  }

  private Role persistRole(String name) {
    return roleRepository.findByName(name).orElseGet(() -> {
      Role role = new Role();
      role.setName(name);
      return roleRepository.save(role);
    });
  }

  private User persistUser(
      String name, String surname, String email, Set<Role> roles, User managerUser) {
    User user = new User();
    user.setName(name);
    user.setSurname(surname);
    user.setEmail(email);
    user.setPasswordHash("hash");
    user.setRoles(new HashSet<>(roles));
    user.setManager(managerUser);
    return userRepository.save(user);
  }
}
