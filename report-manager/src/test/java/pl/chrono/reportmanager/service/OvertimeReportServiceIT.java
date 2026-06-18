package pl.chrono.reportmanager.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;
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

@SpringBootTest
@Transactional
class OvertimeReportServiceIT {

  @Autowired
  private OvertimeReportService overtimeReportService;
  @Autowired
  private UserRepository userRepository;
  @MockitoBean
  private TimesheetReportService timesheetReportService;

  private User manager;
  private User employee1;
  private User employee2;

  @BeforeEach
  void setUp() {
    User mgr = new User(); mgr.setName("Główny"); mgr.setSurname("Menedżer"); mgr.setEmail("manager@chrono.pl"); mgr.setPasswordHash("hash");
    manager = userRepository.save(mgr);

    User emp1 = new User(); emp1.setName("Jan"); emp1.setSurname("Kowalski"); emp1.setEmail("jan@chrono.pl"); emp1.setPasswordHash("hash"); emp1.setManager(manager);
    employee1 = userRepository.save(emp1);

    User emp2 = new User(); emp2.setName("Anna"); emp2.setSurname("Nowak"); emp2.setEmail("anna@chrono.pl"); emp2.setPasswordHash("hash"); emp2.setManager(manager);
    employee2 = userRepository.save(emp2);
  }

  @Test
  void shouldGenerateEmployeeOvertimeReport() {
    LocalDate start = LocalDate.now();
    DailyTimesheetEntry overtimeEntry = new DailyTimesheetEntry(start, LocalTime.of(8,0), LocalTime.of(17,0), 32400L, 28800, 3600L, 0L, null);
    EmployeeTimesheetReport ts = new EmployeeTimesheetReport(employee1.getUserId(), "Jan", start, start, 32400L, 28800L, 3600L, 0L, List.of(overtimeEntry));

    when(timesheetReportService.generateTimesheet(employee1.getUserId(), start, start)).thenReturn(ts);

    EmployeeOvertimeReport report = overtimeReportService.generateEmployeeReport(employee1.getUserId(), start, start);
    assertThat(report.totalOvertimeSeconds()).isEqualTo(3600L);
    assertThat(report.entries()).hasSize(1);
  }

  @Test
  void shouldReturnEmptyEmployeeReportWhenNoOvertime() {
    LocalDate start = LocalDate.now();

    DailyTimesheetEntry normalEntry = new DailyTimesheetEntry(start, LocalTime.of(8,0), LocalTime.of(16,0), 28800L, 28800, 0L, 0L, null);
    EmployeeTimesheetReport ts = new EmployeeTimesheetReport(employee1.getUserId(), "Jan", start, start, 28800L, 28800L, 0L, 0L, List.of(normalEntry));

    when(timesheetReportService.generateTimesheet(employee1.getUserId(), start, start)).thenReturn(ts);

    EmployeeOvertimeReport report = overtimeReportService.generateEmployeeReport(employee1.getUserId(), start, start);
    assertThat(report.totalOvertimeSeconds()).isEqualTo(0L);
    assertThat(report.entries()).isEmpty();
  }

  @Test
  void shouldGenerateTeamOvertimeReport() {
    LocalDate start = LocalDate.now();
    DailyTimesheetEntry overtimeEntry = new DailyTimesheetEntry(start, null, null, 36000L, 28800, 7200L, 0L, null);
    EmployeeTimesheetReport ts = new EmployeeTimesheetReport(employee1.getUserId(), "Jan", start, start, 36000L, 28800L, 7200L, 0L, List.of(overtimeEntry));
    when(timesheetReportService.generateTimesheet(employee1.getUserId(), start, start)).thenReturn(ts);
    when(timesheetReportService.generateTimesheet(employee2.getUserId(), start, start)).thenReturn(new EmployeeTimesheetReport(employee2.getUserId(), "Anna", start, start, 0L, 0L, 0L, 0L, List.of()));

    TeamOvertimeReport report = overtimeReportService.generateTeamReport(manager.getUserId(), start, start);

    assertThat(report.entries()).hasSize(1);
    assertThat(report.entries().get(0).employeeId()).isEqualTo(employee1.getUserId());
  }

  @Test
  void shouldHandleMultipleEmployeesInTeamOvertimeReport() {
    LocalDate start = LocalDate.now();
    DailyTimesheetEntry over1 = new DailyTimesheetEntry(start, null, null, 36000L, 28800, 7200L, 0L, null);
    DailyTimesheetEntry over2 = new DailyTimesheetEntry(start, null, null, 32400L, 28800, 3600L, 0L, null);

    when(timesheetReportService.generateTimesheet(employee1.getUserId(), start, start)).thenReturn(new EmployeeTimesheetReport(employee1.getUserId(), "Jan", start, start, 36000L, 28800L, 7200L, 0L, List.of(over1)));
    when(timesheetReportService.generateTimesheet(employee2.getUserId(), start, start)).thenReturn(new EmployeeTimesheetReport(employee2.getUserId(), "Anna", start, start, 32400L, 28800L, 3600L, 0L, List.of(over2)));

    TeamOvertimeReport report = overtimeReportService.generateTeamReport(manager.getUserId(), start, start);
    assertThat(report.entries()).hasSize(2);
  }

  @Test
  void shouldReturnEmptyTeamReportWhenNoOvertime() {
    LocalDate start = LocalDate.now();
    EmployeeTimesheetReport noOvertimeTs = new EmployeeTimesheetReport(employee1.getUserId(), "Jan", start, start, 28800L, 28800L, 0L, 0L, List.of());
    when(timesheetReportService.generateTimesheet(employee1.getUserId(), start, start)).thenReturn(noOvertimeTs);
    when(timesheetReportService.generateTimesheet(employee2.getUserId(), start, start)).thenReturn(noOvertimeTs);

    TeamOvertimeReport report = overtimeReportService.generateTeamReport(manager.getUserId(), start, start);
    assertThat(report.entries()).isEmpty();
  }
}
