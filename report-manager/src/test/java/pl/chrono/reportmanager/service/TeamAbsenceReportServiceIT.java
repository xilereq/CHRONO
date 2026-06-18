package pl.chrono.reportmanager.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import pl.chrono.dbmanager.model.Application;
import pl.chrono.dbmanager.model.User;
import pl.chrono.dbmanager.model.enums.ApplicationStatus;
import pl.chrono.dbmanager.model.enums.ApplicationType;
import pl.chrono.dbmanager.repository.ApplicationRepository;
import pl.chrono.dbmanager.repository.UserRepository;
import pl.chrono.reportmanager.dto.TeamAbsencesReport;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class TeamAbsenceReportServiceIT {

  @Autowired
  private TeamAbsenceReportService teamAbsenceReportService;
  @Autowired
  private UserRepository userRepository;
  @Autowired
  private ApplicationRepository applicationRepository;

  private User manager;
  private User employee1;
  private User employee2;

  @BeforeEach
  void setUp() {
    User mgr = new User();
    mgr.setName("Szef"); mgr.setSurname("Główny"); mgr.setEmail("szef@chrono.pl"); mgr.setPasswordHash("hash");
    manager = userRepository.save(mgr);

    User emp1 = new User();
    emp1.setName("Piotr"); emp1.setSurname("Zalewski"); emp1.setEmail("piotr@chrono.pl"); emp1.setPasswordHash("hash");
    emp1.setManager(manager);
    employee1 = userRepository.save(emp1);

    User emp2 = new User();
    emp2.setName("Anna"); emp2.setSurname("Nowak"); emp2.setEmail("anna@chrono.pl"); emp2.setPasswordHash("hash");
    emp2.setManager(manager);
    employee2 = userRepository.save(emp2);
  }

  @Test
  void shouldGenerateAbsencesReportForTeam() {
    Application app = createApplication(employee1, ApplicationStatus.APPROVED, LocalDate.of(2023, 1, 10));
    applicationRepository.save(app);

    TeamAbsencesReport report = teamAbsenceReportService.generateReport(manager.getUserId(), LocalDate.of(2023, 1, 1), LocalDate.of(2023, 1, 31));

    assertThat(report.entries()).hasSize(1);
    assertThat(report.entries().get(0).employeeFullName()).isEqualTo("Piotr Zalewski");
  }

  @Test
  void shouldReturnEmptyWhenNoAbsences() {
    TeamAbsencesReport report = teamAbsenceReportService.generateReport(manager.getUserId(), LocalDate.of(2023, 1, 1), LocalDate.of(2023, 1, 31));
    assertThat(report.entries()).isEmpty();
  }

  @Test
  void shouldExcludeAbsencesOutsideDateRange() {
    Application app = createApplication(employee1, ApplicationStatus.APPROVED, LocalDate.of(2022, 12, 10));
    applicationRepository.save(app);

    TeamAbsencesReport report = teamAbsenceReportService.generateReport(manager.getUserId(), LocalDate.of(2023, 1, 1), LocalDate.of(2023, 1, 31));
    assertThat(report.entries()).isEmpty();
  }

  @Test
  void shouldExcludeRejectedAndPendingAbsences() {
    applicationRepository.save(createApplication(employee1, ApplicationStatus.PENDING, LocalDate.of(2023, 1, 10)));
    applicationRepository.save(createApplication(employee2, ApplicationStatus.REJECTED, LocalDate.of(2023, 1, 11)));

    TeamAbsencesReport report = teamAbsenceReportService.generateReport(manager.getUserId(), LocalDate.of(2023, 1, 1), LocalDate.of(2023, 1, 31));

    assertThat(report.entries()).allMatch(e -> e.status().equals("PENDING") || e.status().equals("REJECTED"));
  }

  @Test
  void shouldIncludeMultipleEmployees() {
    applicationRepository.save(createApplication(employee1, ApplicationStatus.APPROVED, LocalDate.of(2023, 1, 10)));
    applicationRepository.save(createApplication(employee2, ApplicationStatus.APPROVED, LocalDate.of(2023, 1, 15)));

    TeamAbsencesReport report = teamAbsenceReportService.generateReport(manager.getUserId(), LocalDate.of(2023, 1, 1), LocalDate.of(2023, 1, 31));
    assertThat(report.entries()).hasSize(2);
  }

  private Application createApplication(User emp, ApplicationStatus status, LocalDate date) {
    Application app = new Application();
    app.setEmployee(emp);
    app.setType(ApplicationType.HOLIDAY_LEAVE);
    app.setStatus(status);
    app.setStartDate(date);
    app.setEndDate(date);
    return app;
  }
}
