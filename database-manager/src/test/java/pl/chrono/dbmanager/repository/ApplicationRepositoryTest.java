package pl.chrono.dbmanager.repository;

import java.time.LocalDate;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import pl.chrono.dbmanager.model.Application;
import pl.chrono.dbmanager.model.User;
import pl.chrono.dbmanager.model.enums.ApplicationStatus;
import pl.chrono.dbmanager.model.enums.ApplicationType;

@DataJpaTest
class ApplicationRepositoryTest {

  @Autowired
  private TestEntityManager em;

  @Autowired
  private ApplicationRepository repository;

  private User manager;
  private User employee;
  private User otherEmployee;

  @BeforeEach
  void setUp() {
    manager = em.persist(buildUser("Kierownik"));
    employee = em.persist(buildUserWithManager("Pracownik", manager));
    otherEmployee = em.persist(buildUser("Inny Pracownik"));
    em.flush();
  }

  @Test
  void findPendingByManagerIdTest() {
    Application app1 = persistApplication(employee, ApplicationStatus.PENDING, LocalDate.now());
    persistApplication(employee, ApplicationStatus.APPROVED, LocalDate.now());

    persistApplication(otherEmployee, ApplicationStatus.PENDING, LocalDate.now());
    em.flush();

    List<Application> result = repository.findPendingByManagerId(
        ApplicationStatus.PENDING, manager.getUserId());

    assertThat(result).hasSize(1).extracting(Application::getId).containsExactly(app1.getId());
  }


  @Test
  void findApprovedApplicationByStartDateTest() {
    LocalDate startDate = LocalDate.of(2026, 6, 10);
    LocalDate endDate = LocalDate.of(2026, 6, 15);
    Application app = persistApplicationWithDates(
        employee, ApplicationStatus.APPROVED, startDate, endDate);
    em.flush();

    List<Application> result = repository.findApprovedByEmployeeAndDate(
        employee.getUserId(), startDate);

    assertThat(result).hasSize(1).extracting(Application::getId).contains(app.getId());
  }

  @Test
  void findApprovedApplicationEndDateTest() {
    LocalDate startDate = LocalDate.of(2026, 6, 10);
    LocalDate endDate = LocalDate.of(2026, 6, 15);
    Application app = persistApplicationWithDates(
        employee, ApplicationStatus.APPROVED, startDate, endDate);
    em.flush();

    List<Application> result = repository.findApprovedByEmployeeAndDate(
        employee.getUserId(), endDate);

    assertThat(result).hasSize(1).extracting(Application::getId).contains(app.getId());
  }

  @Test
  void shouldNotFindApplicationIfDateIsOutsideRange() {
    LocalDate startDate = LocalDate.of(2026, 6, 10);
    LocalDate endDate = LocalDate.of(2026, 6, 15);
    persistApplicationWithDates(employee, ApplicationStatus.APPROVED, startDate, endDate);
    em.flush();

    List<Application> beforeResult = repository.findApprovedByEmployeeAndDate(
        employee.getUserId(), LocalDate.of(2026, 6, 9));
    List<Application> afterResult = repository.findApprovedByEmployeeAndDate(
        employee.getUserId(), LocalDate.of(2026, 6, 16));

    assertThat(beforeResult).isEmpty();
    assertThat(afterResult).isEmpty();
  }

  @Test
  void notApprovedApplicationNotFoundTest() {
    LocalDate today = LocalDate.now();
    persistApplication(employee, ApplicationStatus.PENDING, today);
    em.flush();

    List<Application> result = repository.findApprovedByEmployeeAndDate(
        employee.getUserId(), today);

    assertThat(result).isEmpty();
  }


  @Test
  void allApplicationsFindTest() {
    Application app1 = persistApplication(employee, ApplicationStatus.PENDING, LocalDate.now());
    Application app2 = persistApplication(employee, ApplicationStatus.REJECTED, LocalDate.now());
    persistApplication(otherEmployee, ApplicationStatus.PENDING, LocalDate.now());
    em.flush();

    List<Application> result = repository.findAllByManagerId(manager.getUserId());

    assertThat(result).hasSize(2).extracting(Application::getId)
        .containsExactlyInAnyOrder(app1.getId(), app2.getId());
  }


  private User buildUser(String name) {
    User user = new User();
    user.setName(name);
    user.setSurname("Test");
    user.setEmail(name.toLowerCase().replace(" ", "") + "@test.com");
    user.setPasswordHash("hash");
    return user;
  }

  private User buildUserWithManager(String name, User manager) {
    User user = buildUser(name);
    user.setManager(manager);
    return user;
  }

  private Application persistApplication(User employee, ApplicationStatus status, LocalDate date) {
    return persistApplicationWithDates(employee, status, date, date);
  }

  private Application persistApplicationWithDates(User employee, ApplicationStatus status,
                                                  LocalDate startDate, LocalDate endDate) {
    Application app = new Application();
    app.setEmployee(employee);
    app.setType(ApplicationType.HOLIDAY_LEAVE);
    app.setStatus(status);
    app.setStartDate(startDate);
    app.setEndDate(endDate);
    return em.persist(app);
  }
}
