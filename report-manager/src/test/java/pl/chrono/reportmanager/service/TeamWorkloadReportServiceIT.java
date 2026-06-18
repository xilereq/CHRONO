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
import pl.chrono.reportmanager.dto.TeamWorkloadReport;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@SpringBootTest
@Transactional
class TeamWorkloadReportServiceIT {

  @Autowired
  private TeamWorkloadReportService teamWorkloadReportService;
  @Autowired
  private UserRepository userRepository;
  @MockitoBean
  private TimesheetReportService timesheetReportService;

  private User manager;
  private User employee1;
  private User employee2;

  @BeforeEach
  void setUp() {
    User mgr = new User();
    mgr.setName("Kierownik"); mgr.setSurname("Zespołu"); mgr.setEmail("kierownik@chrono.pl"); mgr.setPasswordHash("hash");
    manager = userRepository.save(mgr);

    User emp1 = new User();
    emp1.setName("Jan"); emp1.setSurname("Kowalski"); emp1.setEmail("jan@chrono.pl"); emp1.setPasswordHash("hash");
    emp1.setManager(manager);
    employee1 = userRepository.save(emp1);

    User emp2 = new User();
    emp2.setName("Adam"); emp2.setSurname("Nowak"); emp2.setEmail("adam@chrono.pl"); emp2.setPasswordHash("hash");
    emp2.setManager(manager);
    employee2 = userRepository.save(emp2);
  }

  @Test
  void shouldGenerateWorkloadReportForSubordinates() {
    LocalDate start = LocalDate.now();
    EmployeeTimesheetReport ts = new EmployeeTimesheetReport(employee1.getUserId(), "Jan Kowalski", start, start, 36000L, 28800L, 7200L, 0L, List.of());
    when(timesheetReportService.generateTimesheet(employee1.getUserId(), start, start)).thenReturn(ts);
    when(timesheetReportService.generateTimesheet(employee2.getUserId(), start, start)).thenReturn(new EmployeeTimesheetReport(employee2.getUserId(), "Adam Nowak", start, start, 0L,0L,0L,0L,List.of()));

    TeamWorkloadReport report = teamWorkloadReportService.generateReport(manager.getUserId(), start, start);

    assertThat(report.entries()).hasSize(2);
    assertThat(report.entries().get(0).employeeFullName()).isEqualTo("Jan Kowalski");
    assertThat(report.entries().get(0).totalWorkSeconds()).isEqualTo(36000L);
  }

  @Test
  void shouldHandleMultipleEmployeesWithDifferentBalances() {
    LocalDate start = LocalDate.now();
    EmployeeTimesheetReport ts1 = new EmployeeTimesheetReport(employee1.getUserId(), "Jan Kowalski", start, start, 36000L, 28800L, 7200L, 0L, List.of());
    EmployeeTimesheetReport ts2 = new EmployeeTimesheetReport(employee2.getUserId(), "Adam Nowak", start, start, 20000L, 28800L, -8800L, 0L, List.of());

    when(timesheetReportService.generateTimesheet(employee1.getUserId(), start, start)).thenReturn(ts1);
    when(timesheetReportService.generateTimesheet(employee2.getUserId(), start, start)).thenReturn(ts2);

    TeamWorkloadReport report = teamWorkloadReportService.generateReport(manager.getUserId(), start, start);

    assertThat(report.entries()).extracting("totalBalanceSeconds").containsExactlyInAnyOrder(7200L, -8800L);
  }

  @Test
  void shouldHandleEmployeeWithNoWork() {
    LocalDate start = LocalDate.now();
    EmployeeTimesheetReport noWorkTs = new EmployeeTimesheetReport(employee1.getUserId(), "Jan Kowalski", start, start, 0L, 28800L, -28800L, 0L, List.of());
    when(timesheetReportService.generateTimesheet(employee1.getUserId(), start, start)).thenReturn(noWorkTs);
    when(timesheetReportService.generateTimesheet(employee2.getUserId(), start, start)).thenReturn(noWorkTs);

    TeamWorkloadReport report = teamWorkloadReportService.generateReport(manager.getUserId(), start, start);
    assertThat(report.entries().get(0).totalWorkSeconds()).isEqualTo(0L);
    assertThat(report.entries().get(0).totalBalanceSeconds()).isEqualTo(-28800L);
  }

  @Test
  void shouldGenerateEmptyReportWhenNoSubordinates() {
    User emptyManager = userRepository.save(createUser("Pusty", "Mgr", "empty@chr.pl"));
    TeamWorkloadReport report = teamWorkloadReportService.generateReport(emptyManager.getUserId(), LocalDate.now(), LocalDate.now());
    assertThat(report.entries()).isEmpty();
  }

  private User createUser(String name, String surname, String email) {
    User u = new User(); u.setName(name); u.setSurname(surname); u.setEmail(email); u.setPasswordHash("h"); return u;
  }
}
