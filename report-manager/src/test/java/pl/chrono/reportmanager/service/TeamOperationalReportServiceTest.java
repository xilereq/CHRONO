package pl.chrono.reportmanager.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pl.chrono.dbmanager.model.User;
import pl.chrono.dbmanager.repository.UserRepository;
import pl.chrono.reportmanager.dto.DailyTimesheetEntry;
import pl.chrono.reportmanager.dto.EmployeeTimesheetReport;
import pl.chrono.reportmanager.dto.TeamDailyOvertimeEntry;
import pl.chrono.reportmanager.dto.TeamDailyOvertimeReport;
import pl.chrono.reportmanager.dto.TeamMatrixReport;
import pl.chrono.reportmanager.dto.TeamNightHoursEntry;
import pl.chrono.reportmanager.dto.TeamNightHoursReport;

@ExtendWith(MockitoExtension.class)
class TeamOperationalReportServiceTest {

  private static final int MANAGER_ID = 1;
  private static final LocalDate DAY_1 = LocalDate.of(2026, 11, 2);
  private static final LocalDate DAY_2 = LocalDate.of(2026, 11, 3);

  @Mock private UserRepository userRepository;
  @Mock private TimesheetReportService timesheetReportService;

  @InjectMocks private TeamOperationalReportService service;

  @BeforeEach
  void givenTeamWithTwoEmployees() {
    when(userRepository.findByManagerUserId(MANAGER_ID)).thenReturn(List.of(user(10), user(20)));
    when(timesheetReportService.generateTimesheet(10, DAY_1, DAY_2)).thenReturn(timesheet(10, "Jan Kowalski",
        day(DAY_1, 28800L, 0L, 3600L),
        day(DAY_2, 32400L, 3600L, 0L)));
    when(timesheetReportService.generateTimesheet(20, DAY_1, DAY_2)).thenReturn(timesheet(20, "Anna Nowak",
        day(DAY_1, 36000L, 7200L, 0L),
        day(DAY_2, 28800L, 0L, 0L)));
  }

  @Test
  void shouldBuildMatrixWithDaysAsRowsAndEmployeesAsColumns() {
    TeamMatrixReport report = service.generateMatrix(MANAGER_ID, DAY_1, DAY_2);

    assertThat(report.employeeNames()).containsExactly("Jan Kowalski", "Anna Nowak");
    assertThat(report.days()).hasSize(2);
    assertThat(report.days().get(0).date()).isEqualTo(DAY_1);
    assertThat(report.days().get(0).workSeconds()).containsExactly(28800L, 36000L);
    assertThat(report.days().get(1).workSeconds()).containsExactly(32400L, 28800L);
  }

  @Test
  void shouldSumTeamOvertimePerDay() {
    TeamDailyOvertimeReport report = service.generateDailyOvertime(MANAGER_ID, DAY_1, DAY_2);

    assertThat(report.entries()).containsExactly(
        new TeamDailyOvertimeEntry(DAY_1, 1, 7200L),
        new TeamDailyOvertimeEntry(DAY_2, 1, 3600L));
  }

  @Test
  void shouldListOnlyEmployeesWithNightHours() {
    TeamNightHoursReport report = service.generateNightHours(MANAGER_ID, DAY_1, DAY_2);

    assertThat(report.entries()).containsExactly(new TeamNightHoursEntry(10, "Jan Kowalski", 1, 3600L));
  }

  private User user(int id) {
    User user = new User();
    user.setUserId(id);
    return user;
  }

  private DailyTimesheetEntry day(LocalDate date, long workSeconds, long balanceSeconds, long nightSeconds) {
    return new DailyTimesheetEntry(date, null, null, workSeconds, 28800, balanceSeconds, nightSeconds, null);
  }

  private EmployeeTimesheetReport timesheet(int id, String name, DailyTimesheetEntry first,
                                            DailyTimesheetEntry second) {
    long work = first.effectiveWorkSeconds() + second.effectiveWorkSeconds();
    long balance = first.balanceSeconds() + second.balanceSeconds();
    long night = first.nightSeconds() + second.nightSeconds();
    return new EmployeeTimesheetReport(id, name, DAY_1, DAY_2, work, 57600L, balance, night, List.of(first, second));
  }
}
