package pl.chrono.reportmanager.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pl.chrono.dbmanager.model.User;
import pl.chrono.dbmanager.repository.UserRepository;
import pl.chrono.reportmanager.dto.DailyTimesheetEntry;
import pl.chrono.reportmanager.dto.EmployeeOvertimeReport;
import pl.chrono.reportmanager.dto.EmployeeTimesheetReport;
import pl.chrono.reportmanager.dto.TeamOvertimeReport;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OvertimeReportServiceTest {

  @Mock
  private TimesheetReportService timesheetReportService;

  @Mock
  private UserRepository userRepository;

  @InjectMocks
  private OvertimeReportService overtimeReportService;

  @Test
  void shouldGenerateEmployeeReport() {
    Integer employeeId = 1;
    LocalDate startDate = LocalDate.of(2023, 1, 1);
    LocalDate endDate = LocalDate.of(2023, 1, 3);

    DailyTimesheetEntry entry1 = new DailyTimesheetEntry(startDate, LocalTime.of(8, 0), LocalTime.of(17, 0), 32400L, 28800, 3600L, 0L, null);
    DailyTimesheetEntry entry2 = new DailyTimesheetEntry(startDate.plusDays(1), LocalTime.of(8, 0), LocalTime.of(16, 0), 28800L, 28800, 0L, 0L, null);
    DailyTimesheetEntry entry3 = new DailyTimesheetEntry(endDate, LocalTime.of(8, 0), LocalTime.of(15, 0), 25200L, 28800, -3600L, 0L, null);

    EmployeeTimesheetReport timesheet = new EmployeeTimesheetReport(employeeId, "Jan Kowalski", startDate, endDate, 86400L, 86400L, 0L, 0L, List.of(entry1, entry2, entry3));

    when(timesheetReportService.generateTimesheet(employeeId, startDate, endDate)).thenReturn(timesheet);

    EmployeeOvertimeReport report = overtimeReportService.generateEmployeeReport(employeeId, startDate, endDate);

    assertThat(report.employeeId()).isEqualTo(employeeId);
    assertThat(report.employeeFullName()).isEqualTo("Jan Kowalski");
    assertThat(report.totalOvertimeSeconds()).isEqualTo(3600L);
    assertThat(report.entries()).hasSize(1);
    assertThat(report.entries().get(0).overtimeSeconds()).isEqualTo(3600L);
  }

  @Test
  void shouldGenerateTeamReport() {
    Integer managerId = 100;
    LocalDate startDate = LocalDate.of(2023, 1, 1);
    LocalDate endDate = LocalDate.of(2023, 1, 31);

    User user1 = new User();
    user1.setUserId(1);
    user1.setName("Jan");
    user1.setSurname("Kowalski");

    User user2 = new User();
    user2.setUserId(2);
    user2.setName("Anna");
    user2.setSurname("Nowak");

    when(userRepository.findByManagerUserId(managerId)).thenReturn(List.of(user1, user2));

    DailyTimesheetEntry entry1 = new DailyTimesheetEntry(startDate, null, null, 36000L, 28800, 7200L, 0L, null);
    EmployeeTimesheetReport timesheet1 = new EmployeeTimesheetReport(1, "Jan Kowalski", startDate, endDate, 36000L, 28800L, 7200L, 0L, List.of(entry1));

    DailyTimesheetEntry entry2 = new DailyTimesheetEntry(startDate, null, null, 28800L, 28800, 0L, 0L, null);
    EmployeeTimesheetReport timesheet2 = new EmployeeTimesheetReport(2, "Anna Nowak", startDate, endDate, 28800L, 28800L, 0L, 0L, List.of(entry2));

    when(timesheetReportService.generateTimesheet(1, startDate, endDate)).thenReturn(timesheet1);
    when(timesheetReportService.generateTimesheet(2, startDate, endDate)).thenReturn(timesheet2);

    TeamOvertimeReport report = overtimeReportService.generateTeamReport(managerId, startDate, endDate);

    assertThat(report.managerId()).isEqualTo(managerId);
    assertThat(report.entries()).hasSize(1);
    assertThat(report.entries().get(0).employeeId()).isEqualTo(1);
    assertThat(report.entries().get(0).totalOvertimeSeconds()).isEqualTo(7200L);
  }
}
