package pl.chrono.reportmanager.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pl.chrono.dbmanager.model.Application;
import pl.chrono.dbmanager.model.User;
import pl.chrono.dbmanager.model.enums.ApplicationStatus;
import pl.chrono.dbmanager.model.enums.ApplicationType;
import pl.chrono.dbmanager.repository.ApplicationRepository;
import pl.chrono.reportmanager.dto.TeamAbsencesReport;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TeamAbsenceReportServiceTest {

  @Mock
  private ApplicationRepository applicationRepository;

  @InjectMocks
  private TeamAbsenceReportService teamAbsenceReportService;

  @Test
  void shouldGenerateAbsencesReport() {
    Integer managerId = 1;
    LocalDate startDate = LocalDate.of(2023, 1, 1);
    LocalDate endDate = LocalDate.of(2023, 1, 31);

    User employee = new User();
    employee.setUserId(5);
    employee.setName("Piotr");
    employee.setSurname("Zalewski");

    Application app = new Application();
    app.setEmployee(employee);
    app.setType(ApplicationType.HOLIDAY_LEAVE);
    app.setStatus(ApplicationStatus.APPROVED);
    app.setStartDate(LocalDate.of(2023, 1, 10));
    app.setEndDate(LocalDate.of(2023, 1, 15));

    when(applicationRepository.findByEmployeeManagerUserIdAndStartDateBetween(managerId, startDate, endDate))
        .thenReturn(List.of(app));

    TeamAbsencesReport report = teamAbsenceReportService.generateReport(managerId, startDate, endDate);

    assertThat(report.managerId()).isEqualTo(managerId);
    assertThat(report.entries()).hasSize(1);

    var entry = report.entries().get(0);
    assertThat(entry.employeeFullName()).isEqualTo("Piotr Zalewski");
    assertThat(entry.applicationType()).isEqualTo("HOLIDAY_LEAVE");
    assertThat(entry.status()).isEqualTo("APPROVED");
    assertThat(entry.startDate()).isEqualTo(LocalDate.of(2023, 1, 10));
  }
}
