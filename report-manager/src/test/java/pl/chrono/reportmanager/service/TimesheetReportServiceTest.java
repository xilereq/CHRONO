package pl.chrono.reportmanager.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pl.chrono.dbmanager.model.Application;
import pl.chrono.dbmanager.model.Policy;
import pl.chrono.dbmanager.model.User;
import pl.chrono.dbmanager.model.WorkLog;
import pl.chrono.dbmanager.model.enums.ApplicationType;
import pl.chrono.dbmanager.repository.ApplicationRepository;
import pl.chrono.dbmanager.repository.UserRepository;
import pl.chrono.dbmanager.repository.WorkLogRepository;
import pl.chrono.reportmanager.dto.EmployeeTimesheetReport;
import pl.chrono.reportmanager.exception.ReportUserException;
import pl.chrono.timemanager.service.WorkTimeCalculationService;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TimesheetReportServiceTest {

  @Mock private UserRepository userRepository;
  @Mock private WorkLogRepository workLogRepository;
  @Mock private ApplicationRepository applicationRepository;
  @Mock private WorkTimeCalculationService workTimeCalculationService;

  @InjectMocks private TimesheetReportService timesheetReportService;

  @Test
  void shouldThrowExceptionIfUserNotFound() {
    when(userRepository.findById(1)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> timesheetReportService.generateTimesheet(1, LocalDate.now(), LocalDate.now()))
        .isInstanceOf(ReportUserException.NotFound.class);
  }

  @Test
  void shouldCalculateCorrectlyForDayWithNightHoursAndOverlap() {
    Integer empId = 1;
    LocalDate date = LocalDate.of(2023, 10, 10);

    User user = mock(User.class);
    when(user.getUserId()).thenReturn(empId);
    when(user.getName()).thenReturn("Jan");
    when(user.getSurname()).thenReturn("Testowy");

    Policy mockPolicy = mock(Policy.class);
    when(user.getPolicyForDate(date)).thenReturn(mockPolicy);
    when(userRepository.findById(empId)).thenReturn(Optional.of(user));

    WorkLog wl = new WorkLog();
    wl.setStartTime(date.atTime(21, 0));
    wl.setEndTime(date.plusDays(1).atTime(3, 0));

    when(workLogRepository.findByUserUserIdAndStartTimeBetween(eq(empId), eq(date.atStartOfDay()), eq(date.atTime(LocalTime.MAX))))
        .thenReturn(Optional.of(wl));

    when(applicationRepository.findApprovedByEmployeeAndDate(eq(empId), eq(date))).thenReturn(List.of());

    when(workTimeCalculationService.getBaseRequiredSeconds(eq(date), any())).thenReturn(28800);
    when(workTimeCalculationService.getRoundingSeconds(any())).thenReturn(0);

    EmployeeTimesheetReport report = timesheetReportService.generateTimesheet(empId, date, date);

    assertThat(report.dailyEntries()).hasSize(1);

    var entry = report.dailyEntries().get(0);

    assertThat(entry.nightSeconds()).isEqualTo(18000L);
    assertThat(report.totalNightSeconds()).isEqualTo(18000L);
  }

  @Test
  void shouldHandleFullDayAbsence() {
    Integer empId = 1;
    LocalDate date = LocalDate.of(2023, 10, 11);

    User user = new User(); user.setUserId(empId); user.setName("A"); user.setSurname("B");
    when(userRepository.findById(empId)).thenReturn(Optional.of(user));

    when(workLogRepository.findByUserUserIdAndStartTimeBetween(eq(empId), any(), any()))
        .thenReturn(Optional.empty());

    Application sickLeave = new Application();
    sickLeave.setType(ApplicationType.SICK_LEAVE);
    when(applicationRepository.findApprovedByEmployeeAndDate(eq(empId), eq(date))).thenReturn(List.of(sickLeave));

    when(workTimeCalculationService.getBaseRequiredSeconds(eq(date), any())).thenReturn(28800);

    EmployeeTimesheetReport report = timesheetReportService.generateTimesheet(empId, date, date);

    var entry = report.dailyEntries().get(0);
    assertThat(entry.applicationType()).isEqualTo("SICK_LEAVE");
    assertThat(entry.effectiveWorkSeconds()).isEqualTo(28800L);
    assertThat(entry.balanceSeconds()).isEqualTo(0L);
  }

  @Test
  void shouldHandleWorkLogWithoutEndTime() {
    Integer empId = 1;
    LocalDate date = LocalDate.of(2023, 10, 12);

    User user = new User(); user.setUserId(empId); user.setName("A"); user.setSurname("B");
    when(userRepository.findById(empId)).thenReturn(Optional.of(user));

    WorkLog wl = new WorkLog();
    wl.setStartTime(date.atTime(8, 0));

    when(workLogRepository.findByUserUserIdAndStartTimeBetween(eq(empId), any(), any())).thenReturn(Optional.of(wl));
    when(applicationRepository.findApprovedByEmployeeAndDate(eq(empId), eq(date))).thenReturn(List.of());
    when(workTimeCalculationService.getBaseRequiredSeconds(any(), any())).thenReturn(28800);

    EmployeeTimesheetReport report = timesheetReportService.generateTimesheet(empId, date, date);

    var entry = report.dailyEntries().get(0);
    assertThat(entry.endTime()).isNull();
    assertThat(entry.nightSeconds()).isEqualTo(0L);
  }

  @Test
  void shouldHandleHolidayLeaveFullDayAbsence() {
    Integer empId = 1;
    LocalDate date = LocalDate.of(2023, 10, 11);

    User user = new User();
    user.setUserId(empId);
    user.setName("A");
    user.setSurname("B");
    when(userRepository.findById(empId)).thenReturn(Optional.of(user));

    when(workLogRepository.findByUserUserIdAndStartTimeBetween(eq(empId), any(), any()))
        .thenReturn(Optional.empty());

    Application holidayLeave = new Application();
    holidayLeave.setType(ApplicationType.HOLIDAY_LEAVE);
    when(applicationRepository.findApprovedByEmployeeAndDate(eq(empId), eq(date))).thenReturn(List.of(holidayLeave));

    when(workTimeCalculationService.getBaseRequiredSeconds(eq(date), any())).thenReturn(28800);

    EmployeeTimesheetReport report = timesheetReportService.generateTimesheet(empId, date, date);

    assertThat(report.dailyEntries().get(0).applicationType()).isEqualTo("HOLIDAY_LEAVE");
    assertThat(report.dailyEntries().get(0).effectiveWorkSeconds()).isEqualTo(28800L);
    assertThat(report.dailyEntries().get(0).balanceSeconds()).isEqualTo(0L);
  }

  @Test
  void shouldNotCountHomeOfficeAsFullDayAbsence() {
    Integer empId = 1;
    LocalDate date = LocalDate.of(2023, 10, 11);

    User user = new User();
    user.setUserId(empId);
    user.setName("A");
    user.setSurname("B");
    when(userRepository.findById(empId)).thenReturn(Optional.of(user));

    when(workLogRepository.findByUserUserIdAndStartTimeBetween(eq(empId), any(), any()))
        .thenReturn(Optional.empty());

    Application homeOffice = new Application();
    homeOffice.setType(ApplicationType.HOME_OFFICE);
    when(applicationRepository.findApprovedByEmployeeAndDate(eq(empId), eq(date))).thenReturn(List.of(homeOffice));

    when(workTimeCalculationService.getBaseRequiredSeconds(eq(date), any())).thenReturn(28800);

    EmployeeTimesheetReport report = timesheetReportService.generateTimesheet(empId, date, date);

    assertThat(report.dailyEntries().get(0).applicationType()).isEqualTo("HOME_OFFICE");
    assertThat(report.dailyEntries().get(0).effectiveWorkSeconds()).isEqualTo(0L);
    assertThat(report.dailyEntries().get(0).balanceSeconds()).isEqualTo(-28800L);
  }
}
