package pl.chrono.reportmanager.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pl.chrono.dbmanager.model.User;
import pl.chrono.dbmanager.repository.UserRepository;
import pl.chrono.reportmanager.dto.EmployeeTimesheetReport;
import pl.chrono.reportmanager.dto.TeamWorkloadReport;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TeamWorkloadReportServiceTest {

  @Mock
  private UserRepository userRepository;

  @Mock
  private TimesheetReportService timesheetReportService;

  @InjectMocks
  private TeamWorkloadReportService teamWorkloadReportService;

  @Test
  void shouldGenerateWorkloadReportForSubordinates() {
    Integer managerId = 1;
    LocalDate startDate = LocalDate.of(2023, 10, 1);
    LocalDate endDate = LocalDate.of(2023, 10, 31);

    User user1 = new User();
    user1.setUserId(10);
    user1.setName("Jan");
    user1.setSurname("Kowalski");

    User user2 = new User();
    user2.setUserId(20);
    user2.setName("Anna");
    user2.setSurname("Nowak");

    when(userRepository.findByManagerUserId(managerId)).thenReturn(List.of(user1, user2));

    EmployeeTimesheetReport timesheet1 = new EmployeeTimesheetReport(10, "Jan Kowalski", startDate, endDate, 36000L, 28800L, 7200L, 0L, List.of());
    EmployeeTimesheetReport timesheet2 = new EmployeeTimesheetReport(20, "Anna Nowak", startDate, endDate, 28000L, 28800L, -800L, 0L, List.of());

    when(timesheetReportService.generateTimesheet(10, startDate, endDate)).thenReturn(timesheet1);
    when(timesheetReportService.generateTimesheet(20, startDate, endDate)).thenReturn(timesheet2);

    TeamWorkloadReport report = teamWorkloadReportService.generateReport(managerId, startDate, endDate);

    assertThat(report.managerId()).isEqualTo(managerId);
    assertThat(report.startDate()).isEqualTo(startDate);
    assertThat(report.endDate()).isEqualTo(endDate);
    assertThat(report.entries()).hasSize(2);

    assertThat(report.entries().get(0).employeeFullName()).isEqualTo("Jan Kowalski");
    assertThat(report.entries().get(0).totalWorkSeconds()).isEqualTo(36000L);
    assertThat(report.entries().get(0).totalBalanceSeconds()).isEqualTo(7200L);

    assertThat(report.entries().get(1).employeeFullName()).isEqualTo("Anna Nowak");
    assertThat(report.entries().get(1).totalBalanceSeconds()).isEqualTo(-800L);
  }

  @Test
  void shouldGenerateEmptyReportWhenNoSubordinates() {
    when(userRepository.findByManagerUserId(anyInt())).thenReturn(Collections.emptyList());

    TeamWorkloadReport report = teamWorkloadReportService.generateReport(1, LocalDate.now(), LocalDate.now());

    assertThat(report.entries()).isEmpty();
    verifyNoInteractions(timesheetReportService);
  }
}
