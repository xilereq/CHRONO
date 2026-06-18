package pl.chrono.reportmanager.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pl.chrono.dbmanager.model.User;
import pl.chrono.dbmanager.repository.UserRepository;
import pl.chrono.reportmanager.dto.EmployeeTimesheetReport;
import pl.chrono.reportmanager.dto.TeamAnomalyReport;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TeamAnomalyReportServiceTest {

  @Mock
  private UserRepository userRepository;

  @Mock
  private TimesheetReportService timesheetReportService;

  @InjectMocks
  private TeamAnomalyReportService teamAnomalyReportService;

  @Test
  void shouldFindAnomaliesCorrectly() {
    Integer managerId = 1;
    LocalDate startDate = LocalDate.of(2023, 10, 1);
    LocalDate endDate = LocalDate.of(2023, 10, 31);

    User u1 = new User(); u1.setUserId(10); u1.setName("A"); u1.setSurname("A");
    User u2 = new User(); u2.setUserId(20); u2.setName("B"); u2.setSurname("B");
    User u3 = new User(); u3.setUserId(30); u3.setName("C"); u3.setSurname("C");

    when(userRepository.findByManagerUserId(managerId)).thenReturn(List.of(u1, u2, u3));

    long required = 10000L;

    EmployeeTimesheetReport ts1 = new EmployeeTimesheetReport(10, "A A", startDate, endDate, 11001L, required, 1001L, 0L, List.of());

    EmployeeTimesheetReport ts2 = new EmployeeTimesheetReport(20, "B B", startDate, endDate, 10999L, required, 999L, 0L, List.of());

    EmployeeTimesheetReport ts3 = new EmployeeTimesheetReport(30, "C C", startDate, endDate, 8500L, required, -1500L, 0L, List.of());

    when(timesheetReportService.generateTimesheet(10, startDate, endDate)).thenReturn(ts1);
    when(timesheetReportService.generateTimesheet(20, startDate, endDate)).thenReturn(ts2);
    when(timesheetReportService.generateTimesheet(30, startDate, endDate)).thenReturn(ts3);

    TeamAnomalyReport report = teamAnomalyReportService.generateReport(managerId, startDate, endDate);

    assertThat(report.entries()).hasSize(2);

    assertThat(report.entries().get(0).employeeId()).isEqualTo(10);
    assertThat(report.entries().get(0).anomalyType()).isEqualTo("ZNACZNE NADGODZINY");

    assertThat(report.entries().get(1).employeeId()).isEqualTo(30);
    assertThat(report.entries().get(1).anomalyType()).isEqualTo("POWAZNY NIEDOBOR");
  }
}
