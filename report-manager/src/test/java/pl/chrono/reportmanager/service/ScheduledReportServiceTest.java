package pl.chrono.reportmanager.service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import static org.assertj.core.api.Assertions.assertThat;
import org.jspecify.annotations.NonNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import pl.chrono.dbmanager.model.Role;
import pl.chrono.dbmanager.model.ScheduledReport;
import pl.chrono.dbmanager.model.User;
import pl.chrono.dbmanager.model.enums.ReportPeriod;
import pl.chrono.dbmanager.model.enums.ScheduleFrequency;
import pl.chrono.dbmanager.model.enums.ScheduleRunStatus;
import pl.chrono.dbmanager.model.enums.ScheduledReportType;
import pl.chrono.dbmanager.ports.I_ScheduledReportData;
import pl.chrono.dbmanager.ports.I_UserData;
import pl.chrono.reportmanager.api.I_ReportFacade;
import pl.chrono.reportmanager.dto.ReportFormat;
import pl.chrono.reportmanager.dto.ScheduledReportRequestDto;
import pl.chrono.reportmanager.dto.ScheduledReportResponseDto;
import pl.chrono.reportmanager.event.ScheduledReportEmailEvent;
import pl.chrono.reportmanager.event.ScheduledReportFailureEvent;
import pl.chrono.reportmanager.exception.ReportScheduleException;
import pl.chrono.reportmanager.exception.ReportScheduleSecurityException;
import pl.chrono.reportmanager.exception.ReportUserException;
import pl.chrono.reportmanager.mapper.ScheduledReportMapper;

@ExtendWith(MockitoExtension.class)
class ScheduledReportServiceTest {

  @Mock
  private I_ScheduledReportData scheduledReportData;

  @Mock
  private I_UserData userData;

  @Mock
  private ScheduledReportMapper scheduledReportMapper;

  @Mock
  private I_ReportFacade reportFacade;

  @Mock
  private ApplicationEventPublisher eventPublisher;

  @InjectMocks
  private ScheduledReportService scheduledReportService;

  private User createUser(Integer id, String name, String surname, String email, String roleName) {
    User user = new User();
    user.setUserId(id);
    user.setName(name);
    user.setSurname(surname);
    user.setEmail(email);
    Role role = new Role();
    role.setName(roleName);
    user.setRoles(new HashSet<>(Set.of(role)));
    return user;
  }

  private ScheduledReportRequestDto createValidDailyRequest(
      ScheduledReportType type, List<Integer> targetIds, List<String> emails) {
    return new ScheduledReportRequestDto(
        "Test Schedule",
        type,
        ReportFormat.PDF,
        ScheduleFrequency.DAILY,
        null,
        null,
        LocalTime.of(8, 0),
        ReportPeriod.PREVIOUS_DAY,
        null,
        null,
        targetIds,
        emails
    );
  }

  private void stubMapperUpdate() {
    doAnswer(invocation -> {
      ScheduledReportRequestDto dto = invocation.getArgument(0);
      ScheduledReport entity = invocation.getArgument(1);
      entity.setName(dto.name());
      entity.setReportType(dto.reportType());
      entity.setReportFormat(dto.reportFormat().name());
      entity.setFrequency(dto.frequency());
      entity.setDayOfWeek(dto.dayOfWeek());
      entity.setDayOfMonth(dto.dayOfMonth());
      entity.setExecutionTime(
          dto.executionTime() != null ? dto.executionTime() : LocalTime.of(7, 0));
      entity.setReportPeriod(dto.reportPeriod());
      entity.setCustomStartDate(dto.customStartDate());
      entity.setCustomEndDate(dto.customEndDate());
      entity.setRecipientEmails(String.join(",", dto.recipientEmails()));
      return null;
    }).when(scheduledReportMapper)
        .updateEntityFromDto(any(ScheduledReportRequestDto.class), any(ScheduledReport.class));
  }

  @Test
  void createScheduleCurrentUserNullTest() {
    ScheduledReportRequestDto dto = createValidDailyRequest(
        ScheduledReportType.EMPLOYEE_TIMESHEET, List.of(1), List.of("jan@chrono.pl"));

    assertThrows(
        ReportUserException.NotFound.class,
        () -> scheduledReportService.createSchedule(null, dto)
    );
  }

  @Test
  void createScheduleCurrentUserNotFoundTest() {
    ScheduledReportRequestDto dto = createValidDailyRequest(
        ScheduledReportType.EMPLOYEE_TIMESHEET, List.of(1), List.of("jan@chrono.pl"));
    when(userData.findById(99)).thenReturn(Optional.empty());

    assertThrows(
        ReportUserException.NotFound.class,
        () -> scheduledReportService.createSchedule(99, dto)
    );
  }

  @Test
  void createScheduleMissingDayOfWeekTest() {
    User employee = createUser(1, "Jan", "Kowalski", "jan@chrono.pl", "ROLE_EMPLOYEE");
    when(userData.findById(1)).thenReturn(Optional.of(employee));

    ScheduledReportRequestDto dto = new ScheduledReportRequestDto(
        "Weekly",
        ScheduledReportType.EMPLOYEE_TIMESHEET,
        ReportFormat.PDF,
        ScheduleFrequency.WEEKLY,
        null,
        null,
        LocalTime.of(7, 0),
        ReportPeriod.PREVIOUS_WEEK,
        null,
        null,
        List.of(1),
        List.of("jan@chrono.pl")
    );

    assertThrows(
        ReportScheduleException.MissingDayOfWeek.class,
        () -> scheduledReportService.createSchedule(1, dto)
    );
  }

  @Test
  void createScheduleMissingDayOfMonthTest() {
    User employee = createUser(1, "Jan", "Kowalski", "jan@chrono.pl", "ROLE_EMPLOYEE");
    when(userData.findById(1)).thenReturn(Optional.of(employee));

    ScheduledReportRequestDto dto = new ScheduledReportRequestDto(
        "Monthly",
        ScheduledReportType.EMPLOYEE_TIMESHEET,
        ReportFormat.PDF,
        ScheduleFrequency.MONTHLY,
        null,
        null,
        LocalTime.of(7, 0),
        ReportPeriod.PREVIOUS_MONTH,
        null,
        null,
        List.of(1),
        List.of("jan@chrono.pl")
    );

    assertThrows(
        ReportScheduleException.MissingDayOfMonth.class,
        () -> scheduledReportService.createSchedule(1, dto)
    );
  }

  @Test
  void createScheduleInvalidCustomDateRangeNullStartTest() {
    User employee = createUser(1, "Jan", "Kowalski", "jan@chrono.pl", "ROLE_EMPLOYEE");
    when(userData.findById(1)).thenReturn(Optional.of(employee));

    ScheduledReportRequestDto dto = new ScheduledReportRequestDto(
        "Custom",
        ScheduledReportType.EMPLOYEE_TIMESHEET,
        ReportFormat.PDF,
        ScheduleFrequency.DAILY,
        null,
        null,
        LocalTime.of(7, 0),
        ReportPeriod.CUSTOM,
        null,
        LocalDate.of(2026, 5, 10),
        List.of(1),
        List.of("jan@chrono.pl")
    );

    assertThrows(
        ReportScheduleException.InvalidCustomDateRange.class,
        () -> scheduledReportService.createSchedule(1, dto)
    );
  }

  @Test
  void createScheduleInvalidCustomDateRangeNullEndTest() {
    User employee = createUser(1, "Jan", "Kowalski", "jan@chrono.pl", "ROLE_EMPLOYEE");
    when(userData.findById(1)).thenReturn(Optional.of(employee));

    ScheduledReportRequestDto dto = new ScheduledReportRequestDto(
        "Custom",
        ScheduledReportType.EMPLOYEE_TIMESHEET,
        ReportFormat.PDF,
        ScheduleFrequency.DAILY,
        null,
        null,
        LocalTime.of(7, 0),
        ReportPeriod.CUSTOM,
        LocalDate.of(2026, 5, 1),
        null,
        List.of(1),
        List.of("jan@chrono.pl")
    );

    assertThrows(
        ReportScheduleException.InvalidCustomDateRange.class,
        () -> scheduledReportService.createSchedule(1, dto)
    );
  }

  @Test
  void createScheduleInvalidCustomDateRangeStartAfterEndTest() {
    User employee = createUser(1, "Jan", "Kowalski", "jan@chrono.pl", "ROLE_EMPLOYEE");
    when(userData.findById(1)).thenReturn(Optional.of(employee));

    ScheduledReportRequestDto dto = new ScheduledReportRequestDto(
        "Custom",
        ScheduledReportType.EMPLOYEE_TIMESHEET,
        ReportFormat.PDF,
        ScheduleFrequency.DAILY,
        null,
        null,
        LocalTime.of(7, 0),
        ReportPeriod.CUSTOM,
        LocalDate.of(2026, 5, 15),
        LocalDate.of(2026, 5, 10),
        List.of(1),
        List.of("jan@chrono.pl")
    );

    assertThrows(
        ReportScheduleException.InvalidCustomDateRange.class,
        () -> scheduledReportService.createSchedule(1, dto)
    );
  }

  @Test
  void createScheduleTargetUsersNotFoundTest() {
    User hr = createUser(1, "Anna", "Nowak", "anna@chrono.pl", "ROLE_HR");
    when(userData.findById(1)).thenReturn(Optional.of(hr));
    when(userData.findAllByIds(List.of(1, 999))).thenReturn(List.of(hr));

    ScheduledReportRequestDto dto = createValidDailyRequest(
        ScheduledReportType.EMPLOYEE_TIMESHEET, List.of(1, 999), List.of("anna@chrono.pl"));

    ReportScheduleException.UsersNotFound ex = assertThrows(
        ReportScheduleException.UsersNotFound.class,
        () -> scheduledReportService.createSchedule(1, dto)
    );
    assertThat(ex.getMessage()).contains("999");
  }

  @Test
  void createScheduleEmployeeTeamReportForbiddenTest() {
    User employee = createUser(1, "Jan", "Kowalski", "jan@chrono.pl", "ROLE_EMPLOYEE");
    when(userData.findById(1)).thenReturn(Optional.of(employee));
    when(userData.findAllByIds(List.of(1))).thenReturn(List.of(employee));

    ScheduledReportRequestDto dto = createValidDailyRequest(
        ScheduledReportType.TEAM_WORKLOAD, List.of(1), List.of("jan@chrono.pl"));

    assertThrows(
        ReportScheduleSecurityException.UnauthorizedTeamReport.class,
        () -> scheduledReportService.createSchedule(1, dto)
    );
  }

  @Test
  void createScheduleEmployeeOtherTargetUserForbiddenTest() {
    User employee = createUser(1, "Jan", "Kowalski", "jan@chrono.pl", "ROLE_EMPLOYEE");
    User other = createUser(2, "Piotr", "Nowak", "piotr@chrono.pl", "ROLE_EMPLOYEE");
    when(userData.findById(1)).thenReturn(Optional.of(employee));
    when(userData.findAllByIds(List.of(2))).thenReturn(List.of(other));

    ScheduledReportRequestDto dto = createValidDailyRequest(
        ScheduledReportType.EMPLOYEE_TIMESHEET, List.of(2), List.of("jan@chrono.pl"));

    assertThrows(
        ReportScheduleSecurityException.UnauthorizedTargetUser.class,
        () -> scheduledReportService.createSchedule(1, dto)
    );
  }

  @Test
  void createScheduleEmployeeMultipleTargetUsersForbiddenTest() {
    User employee = createUser(1, "Jan", "Kowalski", "jan@chrono.pl", "ROLE_EMPLOYEE");
    User other = createUser(2, "Piotr", "Nowak", "piotr@chrono.pl", "ROLE_EMPLOYEE");
    when(userData.findById(1)).thenReturn(Optional.of(employee));
    when(userData.findAllByIds(List.of(1, 2))).thenReturn(List.of(employee, other));

    ScheduledReportRequestDto dto = createValidDailyRequest(
        ScheduledReportType.EMPLOYEE_TIMESHEET, List.of(1, 2), List.of("jan@chrono.pl"));

    assertThrows(
        ReportScheduleSecurityException.UnauthorizedTargetUser.class,
        () -> scheduledReportService.createSchedule(1, dto)
    );
  }

  @Test
  void createScheduleEmployeeForeignEmailForbiddenTest() {
    User employee = createUser(1, "Jan", "Kowalski", "jan@chrono.pl", "ROLE_EMPLOYEE");
    when(userData.findById(1)).thenReturn(Optional.of(employee));
    when(userData.findAllByIds(List.of(1))).thenReturn(List.of(employee));

    ScheduledReportRequestDto dto = createValidDailyRequest(
        ScheduledReportType.EMPLOYEE_TIMESHEET, List.of(1), List.of("other@chrono.pl"));

    assertThrows(
        ReportScheduleSecurityException.UnauthorizedRecipientEmail.class,
        () -> scheduledReportService.createSchedule(1, dto)
    );
  }

  @Test
  void createScheduleEmployeeMultipleEmailsForbiddenTest() {
    User employee = createUser(1, "Jan", "Kowalski", "jan@chrono.pl", "ROLE_EMPLOYEE");
    when(userData.findById(1)).thenReturn(Optional.of(employee));
    when(userData.findAllByIds(List.of(1))).thenReturn(List.of(employee));

    ScheduledReportRequestDto dto = createValidDailyRequest(
        ScheduledReportType.EMPLOYEE_TIMESHEET,
        List.of(1),
        List.of("jan@chrono.pl", "other@chrono.pl")
    );

    assertThrows(
        ReportScheduleSecurityException.UnauthorizedRecipientEmail.class,
        () -> scheduledReportService.createSchedule(1, dto)
    );
  }

  @Test
  void createScheduleManagerNonSubordinateForbiddenTest() {
    User manager = createUser(10, "Marek", "Szef", "marek@chrono.pl", "ROLE_MANAGER");
    User otherManager = createUser(11, "Inny", "Szef", "inny@chrono.pl", "ROLE_MANAGER");
    User nonSubordinate = createUser(2, "Piotr", "Obcy", "piotr@chrono.pl", "ROLE_EMPLOYEE");
    nonSubordinate.setManager(otherManager);

    when(userData.findById(10)).thenReturn(Optional.of(manager));
    when(userData.findAllByIds(List.of(2))).thenReturn(List.of(nonSubordinate));

    ScheduledReportRequestDto dto = createValidDailyRequest(
        ScheduledReportType.EMPLOYEE_TIMESHEET, List.of(2), List.of("marek@chrono.pl"));

    assertThrows(
        ReportScheduleSecurityException.UnauthorizedManagerTargetUsers.class,
        () -> scheduledReportService.createSchedule(10, dto)
    );
  }

  @Test
  void createScheduleManagerTargetWithoutManagerForbiddenTest() {
    User manager = createUser(10, "Marek", "Szef", "marek@chrono.pl", "ROLE_MANAGER");
    User userWithoutManager = createUser(2, "Piotr", "Obcy", "piotr@chrono.pl", "ROLE_EMPLOYEE");

    when(userData.findById(10)).thenReturn(Optional.of(manager));
    when(userData.findAllByIds(List.of(2))).thenReturn(List.of(userWithoutManager));

    ScheduledReportRequestDto dto = createValidDailyRequest(
        ScheduledReportType.EMPLOYEE_TIMESHEET, List.of(2), List.of("marek@chrono.pl"));

    assertThrows(
        ReportScheduleSecurityException.UnauthorizedManagerTargetUsers.class,
        () -> scheduledReportService.createSchedule(10, dto)
    );
  }

  @Test
  void createScheduleEmployeeSuccessTest() {
    User employee = createUser(1, "Jan", "Kowalski", "jan@chrono.pl", "ROLE_EMPLOYEE");
    ScheduledReportRequestDto dto = createValidDailyRequest(
        ScheduledReportType.EMPLOYEE_TIMESHEET, List.of(1), List.of("jan@chrono.pl"));
    ScheduledReportResponseDto expectedResponse = new ScheduledReportResponseDto(
        100L, "Test Schedule", 1, "Jan Kowalski",
        ScheduledReportType.EMPLOYEE_TIMESHEET, ReportFormat.PDF, ScheduleFrequency.DAILY,
        null, null, LocalTime.of(8, 0), ReportPeriod.PREVIOUS_DAY, null, null,
        List.of(1), List.of("jan@chrono.pl"), true, LocalDateTime.now().plusDays(1),
        null, null, 0
    );

    stubMapperUpdate();
    when(userData.findById(1)).thenReturn(Optional.of(employee));
    when(userData.findAllByIds(List.of(1))).thenReturn(List.of(employee));
    when(scheduledReportData.saveScheduledReport(any(ScheduledReport.class)))
        .thenAnswer(inv -> inv.getArgument(0));
    when(scheduledReportMapper.toDto(any(ScheduledReport.class))).thenReturn(expectedResponse);

    ScheduledReportResponseDto result = scheduledReportService.createSchedule(1, dto);

    assertNotNull(result);
    assertEquals(expectedResponse, result);
    verify(scheduledReportData).saveScheduledReport(any(ScheduledReport.class));
  }

  @Test
  void createScheduleManagerForSelfAndSubordinateSuccessTest() {
    User manager = createUser(10, "Marek", "Szef", "marek@chrono.pl", "ROLE_MANAGER");
    User subordinate = createUser(2, "Jan", "Podwladny", "jan@chrono.pl", "ROLE_EMPLOYEE");
    subordinate.setManager(manager);

    ScheduledReportRequestDto dto = new ScheduledReportRequestDto(
        "Custom Manager Schedule",
        ScheduledReportType.TEAM_WORKLOAD,
        ReportFormat.XLSX,
        ScheduleFrequency.WEEKLY,
        1,
        null,
        LocalTime.of(7, 0),
        ReportPeriod.CUSTOM,
        LocalDate.of(2026, 5, 1),
        LocalDate.of(2026, 5, 10),
        List.of(10, 2),
        List.of("marek@chrono.pl")
    );

    stubMapperUpdate();
    when(userData.findById(10)).thenReturn(Optional.of(manager));
    when(userData.findAllByIds(List.of(10, 2))).thenReturn(List.of(manager, subordinate));
    when(scheduledReportData.saveScheduledReport(any(ScheduledReport.class)))
        .thenAnswer(inv -> inv.getArgument(0));
    when(scheduledReportMapper.toDto(any(ScheduledReport.class))).thenReturn(null);

    scheduledReportService.createSchedule(10, dto);

    verify(scheduledReportData).saveScheduledReport(any(ScheduledReport.class));
  }

  @Test
  void createScheduleAdminSuccessTest() {
    User admin = createUser(1, "Admin", "Sys", "admin@chrono.pl", "ROLE_ADMIN");
    User other = createUser(50, "Jan", "Kowalski", "jan@chrono.pl", "ROLE_EMPLOYEE");

    ScheduledReportRequestDto dto = new ScheduledReportRequestDto(
        "Monthly Admin Schedule",
        ScheduledReportType.EMPLOYEE_OVERTIME,
        ReportFormat.CSV,
        ScheduleFrequency.MONTHLY,
        null,
        15,
        LocalTime.of(7, 0),
        ReportPeriod.PREVIOUS_MONTH,
        null,
        null,
        List.of(50),
        List.of("admin@chrono.pl", "hr@chrono.pl")
    );

    stubMapperUpdate();
    when(userData.findById(1)).thenReturn(Optional.of(admin));
    when(userData.findAllByIds(List.of(50))).thenReturn(List.of(other));
    when(scheduledReportData.saveScheduledReport(any(ScheduledReport.class)))
        .thenAnswer(inv -> inv.getArgument(0));

    scheduledReportService.createSchedule(1, dto);

    verify(scheduledReportData).saveScheduledReport(any(ScheduledReport.class));
  }

  @Test
  void updateScheduleSuccessTest() {
    User employee = createUser(1, "Jan", "Kowalski", "jan@chrono.pl", "ROLE_EMPLOYEE");
    ScheduledReport existing = new ScheduledReport();
    existing.setScheduleId(5L);
    existing.setCreator(employee);
    existing.setRetryCount(3);

    ScheduledReportRequestDto dto = createValidDailyRequest(
        ScheduledReportType.EMPLOYEE_TIMESHEET, List.of(1), List.of("jan@chrono.pl"));

    stubMapperUpdate();
    when(userData.findById(1)).thenReturn(Optional.of(employee));
    when(scheduledReportData.findScheduledReportById(5L)).thenReturn(Optional.of(existing));
    when(userData.findAllByIds(List.of(1))).thenReturn(List.of(employee));
    when(scheduledReportData.saveScheduledReport(existing)).thenReturn(existing);

    scheduledReportService.updateSchedule(1, 5L, dto);

    assertEquals(0, existing.getRetryCount());
    assertNotNull(existing.getNextRunAt());
    verify(scheduledReportData).saveScheduledReport(existing);
  }

  @Test
  void getScheduleByIdNotFoundTest() {
    User employee = createUser(1, "Jan", "Kowalski", "jan@chrono.pl", "ROLE_EMPLOYEE");
    when(userData.findById(1)).thenReturn(Optional.of(employee));
    when(scheduledReportData.findScheduledReportById(99L)).thenReturn(Optional.empty());

    assertThrows(
        ReportScheduleException.NotFound.class,
        () -> scheduledReportService.getScheduleById(1, 99L)
    );
  }

  @Test
  void getScheduleByIdUnauthorizedAccessTest() {
    User employee = createUser(1, "Jan", "Kowalski", "jan@chrono.pl", "ROLE_EMPLOYEE");
    User other = createUser(2, "Piotr", "Nowak", "piotr@chrono.pl", "ROLE_EMPLOYEE");
    ScheduledReport schedule = new ScheduledReport();
    schedule.setScheduleId(10L);
    schedule.setCreator(other);

    when(userData.findById(1)).thenReturn(Optional.of(employee));
    when(scheduledReportData.findScheduledReportById(10L)).thenReturn(Optional.of(schedule));

    assertThrows(
        ReportScheduleSecurityException.UnauthorizedScheduleAccess.class,
        () -> scheduledReportService.getScheduleById(1, 10L)
    );
  }

  @Test
  void getScheduleByIdHrCanAccessForeignScheduleTest() {
    User hr = createUser(1, "Anna", "HR", "hr@chrono.pl", "ROLE_HR");
    User other = createUser(2, "Piotr", "Nowak", "piotr@chrono.pl", "ROLE_EMPLOYEE");
    ScheduledReport schedule = new ScheduledReport();
    schedule.setScheduleId(10L);
    schedule.setCreator(other);

    when(userData.findById(1)).thenReturn(Optional.of(hr));
    when(scheduledReportData.findScheduledReportById(10L)).thenReturn(Optional.of(schedule));

    scheduledReportService.getScheduleById(1, 10L);

    verify(scheduledReportMapper).toDto(schedule);
  }

  @Test
  void getSchedulesForHrAndEmployeeTest() {
    User hr = createUser(1, "Anna", "HR", "hr@chrono.pl", "ROLE_HR");
    User employee = createUser(2, "Jan", "Kowalski", "jan@chrono.pl", "ROLE_EMPLOYEE");
    ScheduledReport schedule = new ScheduledReport();

    when(userData.findById(1)).thenReturn(Optional.of(hr));
    when(scheduledReportData.findAllScheduledReports()).thenReturn(List.of(schedule));
    scheduledReportService.getSchedules(1);
    verify(scheduledReportData).findAllScheduledReports();

    when(userData.findById(2)).thenReturn(Optional.of(employee));
    when(scheduledReportData.findAllScheduledReportsByCreatorId(2)).thenReturn(List.of(schedule));
    scheduledReportService.getSchedules(2);
    verify(scheduledReportData).findAllScheduledReportsByCreatorId(2);
  }

  @Test
  void toggleScheduleDeactivateAndActivateTest() {
    User employee = createUser(1, "Jan", "Kowalski", "jan@chrono.pl", "ROLE_EMPLOYEE");
    ScheduledReport schedule = new ScheduledReport();
    schedule.setScheduleId(10L);
    schedule.setCreator(employee);
    schedule.setFrequency(ScheduleFrequency.DAILY);
    schedule.setExecutionTime(LocalTime.of(7, 0));
    schedule.setActive(true);
    schedule.setRetryCount(2);

    when(userData.findById(1)).thenReturn(Optional.of(employee));
    when(scheduledReportData.findScheduledReportById(10L)).thenReturn(Optional.of(schedule));
    when(scheduledReportData.saveScheduledReport(schedule)).thenReturn(schedule);

    scheduledReportService.toggleSchedule(1, 10L);
    assertFalse(schedule.isActive());
    assertEquals(2, schedule.getRetryCount());

    scheduledReportService.toggleSchedule(1, 10L);
    assertTrue(schedule.isActive());
    assertEquals(0, schedule.getRetryCount());
    assertNotNull(schedule.getNextRunAt());
  }

  @Test
  void deleteScheduleTest() {
    User employee = createUser(1, "Jan", "Kowalski", "jan@chrono.pl", "ROLE_EMPLOYEE");
    ScheduledReport schedule = new ScheduledReport();
    schedule.setScheduleId(10L);
    schedule.setCreator(employee);

    when(userData.findById(1)).thenReturn(Optional.of(employee));
    when(scheduledReportData.findScheduledReportById(10L)).thenReturn(Optional.of(schedule));

    scheduledReportService.deleteSchedule(1, 10L);

    verify(scheduledReportData).deleteScheduledReport(schedule);
  }

  @ParameterizedTest
  @EnumSource(ReportPeriod.class)
  void triggerScheduleNowAllPeriodsTest(ReportPeriod period) {
    User creator = createUser(1, "Jan", "Kowalski", "jan@chrono.pl", "ROLE_ADMIN");
    ScheduledReport schedule = getScheduledReport(period, creator);

    when(userData.findById(1)).thenReturn(Optional.of(creator));
    when(scheduledReportData.findScheduledReportById(1L)).thenReturn(Optional.of(schedule));
    when(reportFacade.getEmployeeTimesheet(any(), any(), any(), eq(ReportFormat.PDF)))
        .thenReturn(new byte[]{1, 2, 3});

    scheduledReportService.triggerScheduleNow(1, 1L);

    LocalDate today = LocalDate.now();
    LocalDate expectedStart;
    LocalDate expectedEnd;
    switch (period) {
      case PREVIOUS_DAY -> {
        expectedStart = today.minusDays(1);
        expectedEnd = today.minusDays(1);
      }
      case CURRENT_WEEK -> {
        expectedStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        expectedEnd = today;
      }
      case PREVIOUS_WEEK -> {
        expectedStart =
            today.minusWeeks(1).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        expectedEnd = expectedStart.plusDays(6);
      }
      case CURRENT_MONTH -> {
        expectedStart = today.withDayOfMonth(1);
        expectedEnd = today;
      }
      case PREVIOUS_MONTH -> {
        LocalDate prevMonth = today.minusMonths(1);
        expectedStart = prevMonth.withDayOfMonth(1);
        expectedEnd = prevMonth.with(TemporalAdjusters.lastDayOfMonth());
      }
      case LAST_3_MONTHS -> {
        expectedStart = today.minusMonths(3);
        expectedEnd = today;
      }
      case LAST_6_MONTHS -> {
        expectedStart = today.minusMonths(6);
        expectedEnd = today;
      }
      case LAST_YEAR -> {
        expectedStart = today.minusYears(1);
        expectedEnd = today;
      }
      case CUSTOM -> {
        expectedStart = LocalDate.of(2026, 1, 1);
        expectedEnd = LocalDate.of(2026, 1, 31);
      }
      default -> throw new IllegalStateException();
    }

    verify(reportFacade).getEmployeeTimesheet(1, expectedStart, expectedEnd, ReportFormat.PDF);
    assertEquals(ScheduleRunStatus.SUCCESS, schedule.getLastRunStatus());
    assertNotNull(schedule.getLastRunAt());

    ArgumentCaptor<ScheduledReportEmailEvent> eventCaptor =
        ArgumentCaptor.forClass(ScheduledReportEmailEvent.class);
    verify(eventPublisher).publishEvent(eventCaptor.capture());
    assertThat(eventCaptor.getValue().recipientEmails()).containsExactly("jan@chrono.pl", "hr@chrono.pl");
  }

  private static @NonNull ScheduledReport getScheduledReport(ReportPeriod period, User creator) {
    ScheduledReport schedule = new ScheduledReport();
    schedule.setScheduleId(1L);
    schedule.setName("Raport Okresowy");
    schedule.setCreator(creator);
    schedule.setReportType(ScheduledReportType.EMPLOYEE_TIMESHEET);
    schedule.setReportFormat("PDF");
    schedule.setReportPeriod(period);
    schedule.setCustomStartDate(LocalDate.of(2026, 1, 1));
    schedule.setCustomEndDate(LocalDate.of(2026, 1, 31));
    schedule.setRecipientEmails("jan@chrono.pl, , hr@chrono.pl ");
    schedule.setTargetUsers(Set.of(creator));
    return schedule;
  }

  @ParameterizedTest
  @EnumSource(ScheduledReportType.class)
  void invokeAllReportTypesAndFormatsTest(ScheduledReportType reportType) {
    User creator = createUser(1, "Jan", "Kowalski", "jan@chrono.pl", "ROLE_ADMIN");
    ReportFormat format = switch (reportType) {
      case EMPLOYEE_TIMESHEET, EMPLOYEE_OVERTIME -> ReportFormat.CSV;
      case TEAM_WORKLOAD, TEAM_ABSENCES -> ReportFormat.XLSX;
      case TEAM_OVERTIME, TEAM_ANOMALY -> ReportFormat.PDF;
    };

    ScheduledReport schedule = new ScheduledReport();
    schedule.setScheduleId(1L);
    schedule.setName("Raport");
    schedule.setCreator(creator);
    schedule.setReportType(reportType);
    schedule.setReportFormat(format.name());
    schedule.setReportPeriod(ReportPeriod.PREVIOUS_DAY);
    schedule.setRecipientEmails("jan@chrono.pl");
    schedule.setTargetUsers(Set.of(creator));

    when(userData.findById(1)).thenReturn(Optional.of(creator));
    when(scheduledReportData.findScheduledReportById(1L)).thenReturn(Optional.of(schedule));

    scheduledReportService.triggerScheduleNow(1, 1L);

    verify(scheduledReportData).saveScheduledReport(schedule);
  }

  @Test
  void processDueSchedulesEmptyAndNonEmptyTest() {
    when(scheduledReportData.findAllDueScheduledReports(any(LocalDateTime.class)))
        .thenReturn(Collections.emptyList());

    scheduledReportService.processDueSchedules();
    verify(scheduledReportData, never()).saveScheduledReport(any());

    User creator = createUser(1, "Jan", "Kowalski", "jan@chrono.pl", "ROLE_EMPLOYEE");
    ScheduledReport schedule = new ScheduledReport();
    schedule.setScheduleId(10L);
    schedule.setName("Daily");
    schedule.setCreator(creator);
    schedule.setReportType(ScheduledReportType.EMPLOYEE_TIMESHEET);
    schedule.setReportFormat("CSV");
    schedule.setFrequency(ScheduleFrequency.DAILY);
    schedule.setExecutionTime(LocalTime.of(7, 0));
    schedule.setReportPeriod(ReportPeriod.PREVIOUS_DAY);
    schedule.setRecipientEmails("jan@chrono.pl");
    schedule.setTargetUsers(Set.of(creator));

    when(scheduledReportData.findAllDueScheduledReports(any(LocalDateTime.class)))
        .thenReturn(List.of(schedule));

    scheduledReportService.processDueSchedules();

    assertEquals(ScheduleRunStatus.SUCCESS, schedule.getLastRunStatus());
    assertEquals(0, schedule.getRetryCount());
    verify(scheduledReportData).saveScheduledReport(schedule);
  }

  @Test
  void processSingleScheduleExecutionRetryBelowMaxAndAtMaxTest() {
    User creator = createUser(1, "Jan", "Kowalski", "jan@chrono.pl", "ROLE_EMPLOYEE");
    ScheduledReport schedule = new ScheduledReport();
    schedule.setScheduleId(10L);
    schedule.setName("Awaryjny Raport");
    schedule.setCreator(creator);
    schedule.setReportType(ScheduledReportType.EMPLOYEE_TIMESHEET);
    schedule.setReportFormat("PDF");
    schedule.setFrequency(ScheduleFrequency.DAILY);
    schedule.setExecutionTime(LocalTime.of(7, 0));
    schedule.setReportPeriod(ReportPeriod.PREVIOUS_DAY);
    schedule.setRecipientEmails("jan@chrono.pl");
    schedule.setTargetUsers(Set.of(creator));
    schedule.setRetryCount(0);
    LocalDateTime initialNextRun = LocalDateTime.of(2026, 5, 1, 7, 0);
    schedule.setNextRunAt(initialNextRun);

    when(reportFacade.getEmployeeTimesheet(any(), any(), any(), any()))
        .thenThrow(new RuntimeException("Generator error"));

    scheduledReportService.processSingleScheduleExecution(schedule, initialNextRun);

    assertEquals(1, schedule.getRetryCount());
    assertEquals(ScheduleRunStatus.FAILED, schedule.getLastRunStatus());
    assertEquals(initialNextRun, schedule.getNextRunAt());
    verify(eventPublisher, never()).publishEvent(any(ScheduledReportFailureEvent.class));

    schedule.setRetryCount(4);
    scheduledReportService.processSingleScheduleExecution(schedule, initialNextRun);

    assertEquals(0, schedule.getRetryCount());
    assertEquals(ScheduleRunStatus.FAILED, schedule.getLastRunStatus());
    assertEquals(LocalDateTime.of(2026, 5, 2, 7, 0), schedule.getNextRunAt());

    ArgumentCaptor<ScheduledReportFailureEvent> failureCaptor =
        ArgumentCaptor.forClass(ScheduledReportFailureEvent.class);
    verify(eventPublisher).publishEvent(failureCaptor.capture());
    assertEquals(1, failureCaptor.getValue().creatorId());
    assertEquals(10L, failureCaptor.getValue().scheduleId());
  }

  @Test
  void calculateNextRunAtDailyWeeklyMonthlyTest() {
    ScheduledReport daily = new ScheduledReport();
    daily.setFrequency(ScheduleFrequency.DAILY);
    daily.setExecutionTime(null);

    assertEquals(
        LocalDateTime.of(2026, 5, 1, 7, 0),
        scheduledReportService.calculateNextRunAt(daily, LocalDateTime.of(2026, 5, 1, 6, 0))
    );
    assertEquals(
        LocalDateTime.of(2026, 5, 2, 7, 0),
        scheduledReportService.calculateNextRunAt(daily, LocalDateTime.of(2026, 5, 1, 7, 0))
    );

    ScheduledReport weekly = new ScheduledReport();
    weekly.setFrequency(ScheduleFrequency.WEEKLY);
    weekly.setDayOfWeek(null);
    weekly.setExecutionTime(LocalTime.of(8, 0));

    assertEquals(
        LocalDateTime.of(2026, 5, 4, 8, 0),
        scheduledReportService.calculateNextRunAt(weekly, LocalDateTime.of(2026, 5, 4, 7, 0))
    );
    weekly.setDayOfWeek(1);
    assertEquals(
        LocalDateTime.of(2026, 5, 11, 8, 0),
        scheduledReportService.calculateNextRunAt(weekly, LocalDateTime.of(2026, 5, 4, 9, 0))
    );

    ScheduledReport monthly = new ScheduledReport();
    monthly.setFrequency(ScheduleFrequency.MONTHLY);
    monthly.setDayOfMonth(null);
    monthly.setExecutionTime(LocalTime.of(9, 0));

    assertEquals(
        LocalDateTime.of(2026, 5, 1, 9, 0),
        scheduledReportService.calculateNextRunAt(monthly, LocalDateTime.of(2026, 5, 1, 8, 0))
    );

    monthly.setDayOfMonth(31);
    assertEquals(
        LocalDateTime.of(2026, 2, 28, 9, 0),
        scheduledReportService.calculateNextRunAt(monthly, LocalDateTime.of(2026, 2, 10, 10, 0))
    );
    assertEquals(
        LocalDateTime.of(2026, 3, 28, 9, 0),
        scheduledReportService.calculateNextRunAt(monthly, LocalDateTime.of(2026, 2, 28, 10, 0))
    );
  }
}
