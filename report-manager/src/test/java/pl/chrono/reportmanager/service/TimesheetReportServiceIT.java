package pl.chrono.reportmanager.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;
import pl.chrono.dbmanager.model.Application;
import pl.chrono.dbmanager.model.User;
import pl.chrono.dbmanager.model.WorkLog;
import pl.chrono.dbmanager.model.enums.ApplicationStatus;
import pl.chrono.dbmanager.model.enums.ApplicationType;
import pl.chrono.dbmanager.repository.ApplicationRepository;
import pl.chrono.dbmanager.repository.UserRepository;
import pl.chrono.dbmanager.repository.WorkLogRepository;
import pl.chrono.reportmanager.dto.EmployeeTimesheetReport;
import pl.chrono.reportmanager.exception.ReportUserException;
import pl.chrono.timemanager.service.WorkTimeCalculationService;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@SpringBootTest
@Transactional
class TimesheetReportServiceIT {

  @Autowired
  private TimesheetReportService timesheetReportService;
  @Autowired
  private UserRepository userRepository;
  @Autowired
  private WorkLogRepository workLogRepository;
  @Autowired
  private ApplicationRepository applicationRepository;
  @MockitoBean
  private WorkTimeCalculationService workTimeCalculationService;

  private User employee;

  @BeforeEach
  void setUp() {
    User user = new User();
    user.setName("Jan");
    user.setSurname("Kowalski");
    user.setEmail("jan.kowalski@chrono.pl");
    user.setPasswordHash("dummyHash");
    employee = userRepository.save(user);

    when(workTimeCalculationService.getBaseRequiredSeconds(any(LocalDate.class), any())).thenReturn(28800);
    when(workTimeCalculationService.getRoundingSeconds(any())).thenReturn(0);
  }

  @Test
  void shouldGenerateTimesheetWithWorkLogs() {
    LocalDate targetDate = LocalDate.of(2023, 10, 10);
    WorkLog workLog = new WorkLog();
    workLog.setUser(employee);
    workLog.setStartTime(targetDate.atTime(8, 0));
    workLog.setEndTime(targetDate.atTime(16, 0));
    workLogRepository.save(workLog);

    EmployeeTimesheetReport report = timesheetReportService.generateTimesheet(employee.getUserId(), targetDate, targetDate);

    assertThat(report.dailyEntries()).hasSize(1);
    assertThat(report.dailyEntries().get(0).effectiveWorkSeconds()).isEqualTo(28800L);
    assertThat(report.totalBalanceSeconds()).isEqualTo(0L);
  }

  @Test
  void shouldHandleDayWithoutWorkLogAndAbsence() {
    LocalDate targetDate = LocalDate.of(2023, 10, 10);
    EmployeeTimesheetReport report = timesheetReportService.generateTimesheet(employee.getUserId(), targetDate, targetDate);

    assertThat(report.dailyEntries().get(0).effectiveWorkSeconds()).isEqualTo(0L);
    assertThat(report.dailyEntries().get(0).balanceSeconds()).isEqualTo(-28800L);
  }

  @Test
  void shouldHandleWorkLogWithoutEndTime() {
    LocalDate targetDate = LocalDate.of(2023, 10, 10);
    WorkLog log = new WorkLog();
    log.setUser(employee);
    log.setStartTime(targetDate.atTime(8, 0));
    workLogRepository.save(log);

    EmployeeTimesheetReport report = timesheetReportService.generateTimesheet(employee.getUserId(), targetDate, targetDate);

    assertThat(report.dailyEntries().get(0).endTime()).isNull();
    assertThat(report.dailyEntries().get(0).nightSeconds()).isEqualTo(0L);
  }

  @Test
  void shouldCalculateCorrectlyForDayWithNightHours() {
    LocalDate targetDate = LocalDate.of(2023, 10, 10);
    WorkLog workLog = new WorkLog();
    workLog.setUser(employee);
    workLog.setStartTime(targetDate.atTime(21, 0));
    workLog.setEndTime(targetDate.plusDays(1).atTime(3, 0));
    workLogRepository.save(workLog);

    EmployeeTimesheetReport report = timesheetReportService.generateTimesheet(employee.getUserId(), targetDate, targetDate);

    assertThat(report.totalNightSeconds()).isGreaterThan(0L);
  }

  @Test
  void shouldHandleAbsenceInTimesheet() {
    LocalDate targetDate = LocalDate.of(2023, 10, 11);
    Application sickLeave = new Application();
    sickLeave.setEmployee(employee);
    sickLeave.setType(ApplicationType.SICK_LEAVE);
    sickLeave.setStatus(ApplicationStatus.APPROVED);
    sickLeave.setStartDate(targetDate);
    sickLeave.setEndDate(targetDate);
    applicationRepository.save(sickLeave);

    EmployeeTimesheetReport report = timesheetReportService.generateTimesheet(employee.getUserId(), targetDate, targetDate);

    assertThat(report.dailyEntries().get(0).applicationType()).isEqualTo("SICK_LEAVE");
    assertThat(report.dailyEntries().get(0).effectiveWorkSeconds()).isEqualTo(28800L);
  }

  @Test
  void shouldThrowExceptionIfUserNotFound() {
    assertThatThrownBy(() -> timesheetReportService.generateTimesheet(999, LocalDate.now(), LocalDate.now()))
        .isInstanceOf(ReportUserException.NotFound.class);
  }
}
