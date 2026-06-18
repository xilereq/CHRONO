package pl.chrono.reportmanager.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;
import pl.chrono.dbmanager.model.User;
import pl.chrono.dbmanager.repository.UserRepository;
import pl.chrono.reportmanager.dto.EmployeeTimesheetReport;
import pl.chrono.reportmanager.dto.TeamAnomalyReport;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@SpringBootTest
@Transactional
class TeamAnomalyReportServiceIT {

  @Autowired
  private TeamAnomalyReportService teamAnomalyReportService;
  @Autowired
  private UserRepository userRepository;
  @MockitoBean
  private TimesheetReportService timesheetReportService;

  private User manager;
  private User emp1;
  private User emp2;

  @BeforeEach
  void setUp() {
    User mgr = new User(); mgr.setName("Szef"); mgr.setSurname("Działu"); mgr.setEmail("szef@chrono.pl"); mgr.setPasswordHash("hash");
    manager = userRepository.save(mgr);

    User e1 = new User(); e1.setName("Adam"); e1.setSurname("Pracowity"); e1.setEmail("adam@chrono.pl"); e1.setPasswordHash("h"); e1.setManager(manager);
    emp1 = userRepository.save(e1);

    User e2 = new User(); e2.setName("Marek"); e2.setSurname("Leniwy"); e2.setEmail("marek@chrono.pl"); e2.setPasswordHash("h"); e2.setManager(manager);
    emp2 = userRepository.save(e2);
  }

  @Test
  void shouldFindAnomaliesInTeam() {
    LocalDate start = LocalDate.now();
    long required = 10000L;
    EmployeeTimesheetReport tsOvertime = new EmployeeTimesheetReport(emp1.getUserId(), "Adam Pracowity", start, start, 12000L, required, 2000L, 0L, List.of());
    EmployeeTimesheetReport tsDeficit = new EmployeeTimesheetReport(emp2.getUserId(), "Marek Leniwy", start, start, 8000L, required, -2000L, 0L, List.of());

    when(timesheetReportService.generateTimesheet(emp1.getUserId(), start, start)).thenReturn(tsOvertime);
    when(timesheetReportService.generateTimesheet(emp2.getUserId(), start, start)).thenReturn(tsDeficit);

    TeamAnomalyReport report = teamAnomalyReportService.generateReport(manager.getUserId(), start, start);

    assertThat(report.entries()).hasSize(2);
    assertThat(report.entries()).anyMatch(e -> e.anomalyType().equals("ZNACZNE NADGODZINY"));
    assertThat(report.entries()).anyMatch(e -> e.anomalyType().equals("POWAZNY NIEDOBOR"));
  }

  @Test
  void shouldReturnEmptyWhenNoAnomalies() {
    LocalDate start = LocalDate.now();
    EmployeeTimesheetReport perfectTs1 = new EmployeeTimesheetReport(emp1.getUserId(), "Adam Pracowity", start, start, 10000L, 10000L, 0L, 0L, List.of());
    EmployeeTimesheetReport perfectTs2 = new EmployeeTimesheetReport(emp2.getUserId(), "Marek Leniwy", start, start, 10000L, 10000L, 0L, 0L, List.of());

    when(timesheetReportService.generateTimesheet(emp1.getUserId(), start, start)).thenReturn(perfectTs1);
    when(timesheetReportService.generateTimesheet(emp2.getUserId(), start, start)).thenReturn(perfectTs2);

    TeamAnomalyReport report = teamAnomalyReportService.generateReport(manager.getUserId(), start, start);
    assertThat(report.entries()).isEmpty();
  }

  @Test
  void shouldHandleMultipleEmployeesWithSameAnomaly() {
    LocalDate start = LocalDate.now();
    EmployeeTimesheetReport tsOvertime1 = new EmployeeTimesheetReport(emp1.getUserId(), "Adam", start, start, 12000L, 10000L, 2000L, 0L, List.of());
    EmployeeTimesheetReport tsOvertime2 = new EmployeeTimesheetReport(emp2.getUserId(), "Marek", start, start, 12000L, 10000L, 2000L, 0L, List.of());

    when(timesheetReportService.generateTimesheet(emp1.getUserId(), start, start)).thenReturn(tsOvertime1);
    when(timesheetReportService.generateTimesheet(emp2.getUserId(), start, start)).thenReturn(tsOvertime2);

    TeamAnomalyReport report = teamAnomalyReportService.generateReport(manager.getUserId(), start, start);
    assertThat(report.entries()).hasSize(2);
    assertThat(report.entries()).allMatch(e -> e.anomalyType().equals("ZNACZNE NADGODZINY"));
  }

  @Test
  void shouldReturnEmptyReportWhenNoSubordinates() {
    User emptyManager = new User(); emptyManager.setName("x"); emptyManager.setSurname("y"); emptyManager.setEmail("x@x.pl"); emptyManager.setPasswordHash("h");
    userRepository.save(emptyManager);

    TeamAnomalyReport report = teamAnomalyReportService.generateReport(emptyManager.getUserId(), LocalDate.now(), LocalDate.now());
    assertThat(report.entries()).isEmpty();
  }
}
