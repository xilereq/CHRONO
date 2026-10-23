package pl.chrono.dbmanager.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.hibernate.Hibernate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import pl.chrono.dbmanager.model.ScheduledReport;
import pl.chrono.dbmanager.model.User;
import pl.chrono.dbmanager.model.enums.ReportPeriod;
import pl.chrono.dbmanager.model.enums.ScheduleFrequency;
import pl.chrono.dbmanager.model.enums.ScheduledReportType;

@DataJpaTest
class ScheduledReportRepositoryIT {

  @Autowired
  private TestEntityManager em;

  @Autowired
  private ScheduledReportRepository repository;

  private User creator1;
  private User creator2;
  private User targetUser;

  @BeforeEach
  void setUp() {
    creator1 = persistUser("Jan", "Kowalski", "jan.kowalski@chrono.pl");
    creator2 = persistUser("Anna", "Nowak", "anna.nowak@chrono.pl");
    targetUser = persistUser("Piotr", "Zieliński", "piotr.zielinski@chrono.pl");
    em.flush();
  }

  @Test
  void findAllDueWithDetailsReturnsOnlyActiveAndDueWithFetchedAssociationsTest() {
    LocalDateTime now = LocalDateTime.of(2026, 5, 10, 12, 0);

    ScheduledReport dueActive = persistSchedule(
        "Due Active", creator1, Set.of(creator1, targetUser),
        true, now.minusMinutes(5), now.minusDays(2)
    );
    ScheduledReport dueExactNow = persistSchedule(
        "Due Exact Now", creator1, Set.of(creator1),
        true, now, now.minusDays(1)
    );
    persistSchedule(
        "Inactive Due", creator1, Set.of(creator1),
        false, now.minusMinutes(10), now.minusDays(1)
    );
    persistSchedule(
        "Future Active", creator1, Set.of(creator1),
        true, now.plusMinutes(10), now.minusDays(1)
    );

    em.flush();
    em.clear();

    List<ScheduledReport> result = repository.findAllDueWithDetails(now);
    em.clear();

    assertThat(result)
        .extracting(ScheduledReport::getScheduleId)
        .containsExactlyInAnyOrder(dueActive.getScheduleId(), dueExactNow.getScheduleId());

    for (ScheduledReport report : result) {
      assertThat(Hibernate.isInitialized(report.getCreator())).isTrue();
      assertThat(Hibernate.isInitialized(report.getTargetUsers())).isTrue();
      assertThat(report.getCreator().getEmail()).isEqualTo("jan.kowalski@chrono.pl");
      assertThat(report.getTargetUsers()).isNotEmpty();
    }
  }

  @Test
  void findAllByCreatorIdWithDetailsOrdersByCreatedAtDescAndFetchesAssociationsTest() {
    LocalDateTime baseTime = LocalDateTime.of(2026, 5, 1, 10, 0);

    ScheduledReport older = persistSchedule(
        "Older Schedule", creator1, Set.of(creator1),
        true, baseTime.plusDays(1), baseTime.minusHours(2)
    );
    ScheduledReport newer = persistSchedule(
        "Newer Schedule", creator1, Set.of(creator1, targetUser),
        true, baseTime.plusDays(1), baseTime
    );
    persistSchedule(
        "Other Creator Schedule", creator2, Set.of(creator2),
        true, baseTime.plusDays(1), baseTime.plusHours(1)
    );

    em.flush();
    em.clear();

    List<ScheduledReport> result = repository.findAllByCreatorIdWithDetails(creator1.getUserId());
    em.clear();

    assertThat(result)
        .extracting(ScheduledReport::getScheduleId)
        .containsExactly(newer.getScheduleId(), older.getScheduleId());

    assertThat(result.getFirst().getCreator().getName()).isEqualTo("Jan");
    assertThat(result.getFirst().getTargetUsers()).hasSize(2);
  }

  @Test
  void findAllWithDetailsOrdersByCreatedAtDescAndFetchesAssociationsTest() {
    LocalDateTime baseTime = LocalDateTime.of(2026, 5, 1, 10, 0);

    ScheduledReport first = persistSchedule(
        "First", creator1, Set.of(creator1),
        true, baseTime.plusDays(1), baseTime.minusHours(3)
    );
    ScheduledReport second = persistSchedule(
        "Second", creator2, Set.of(creator2, targetUser),
        true, baseTime.plusDays(1), baseTime.minusHours(1)
    );

    em.flush();
    em.clear();

    List<ScheduledReport> result = repository.findAllWithDetails();
    em.clear();

    assertThat(result)
        .extracting(ScheduledReport::getScheduleId)
        .containsExactly(second.getScheduleId(), first.getScheduleId());

    assertThat(result.getFirst().getCreator().getEmail()).isEqualTo("anna.nowak@chrono.pl");
    assertThat(result.getFirst().getTargetUsers()).hasSize(2);
  }

  @Test
  void findByIdWithDetailsFetchesCreatorAndTargetUsersWithoutLazyExceptionTest() {
    LocalDateTime now = LocalDateTime.of(2026, 5, 1, 10, 0);
    ScheduledReport saved = persistSchedule(
        "Single Schedule", creator1, Set.of(creator1, targetUser),
        true, now.plusDays(1), now
    );

    em.flush();
    em.clear();

    Optional<ScheduledReport> foundOpt = repository.findByIdWithDetails(saved.getScheduleId());
    em.clear();

    assertThat(foundOpt).isPresent();
    ScheduledReport found = foundOpt.get();
    assertThat(found.getCreator().getName()).isEqualTo("Jan");
    assertThat(found.getTargetUsers())
        .extracting(User::getEmail)
        .containsExactlyInAnyOrder("jan.kowalski@chrono.pl", "piotr.zielinski@chrono.pl");

    assertThat(repository.findByIdWithDetails(999999L)).isEmpty();
  }

  private User persistUser(String name, String surname, String email) {
    User user = new User();
    user.setName(name);
    user.setSurname(surname);
    user.setEmail(email);
    user.setPasswordHash("secret");
    return em.persist(user);
  }

  private ScheduledReport persistSchedule(
      String name,
      User creator,
      Set<User> targets,
      boolean active,
      LocalDateTime nextRunAt,
      LocalDateTime createdAt) {
    ScheduledReport sr = new ScheduledReport();
    sr.setName(name);
    sr.setCreator(creator);
    sr.setReportType(ScheduledReportType.EMPLOYEE_TIMESHEET);
    sr.setReportFormat("PDF");
    sr.setFrequency(ScheduleFrequency.DAILY);
    sr.setExecutionTime(LocalTime.of(7, 0));
    sr.setReportPeriod(ReportPeriod.PREVIOUS_DAY);
    sr.setRecipientEmails(creator.getEmail());
    sr.setActive(active);
    sr.setNextRunAt(nextRunAt);
    sr.setCreatedAt(createdAt);
    sr.setTargetUsers(new HashSet<>(targets));
    return em.persist(sr);
  }
}
