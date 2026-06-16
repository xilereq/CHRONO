package pl.chrono.dbmanager.repository;

import java.time.LocalDate;
import java.util.Set;
import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import pl.chrono.dbmanager.model.Holiday;

@DataJpaTest
class HolidayRepositoryIT {

  @Autowired
  private TestEntityManager em;

  @Autowired
  private HolidayRepository repository;

  @Test
  void existsByDateOrYearlyExactMatchTest() {
    persistHoliday(LocalDate.of(2026, 5, 1), false);
    em.flush();

    boolean exists = repository.existsByDateOrYearly(LocalDate.of(2026, 5, 1), 5, 1);
    boolean notExists = repository.existsByDateOrYearly(LocalDate.of(2026, 5, 2), 5, 2);

    assertThat(exists).isTrue();
    assertThat(notExists).isFalse();
  }

  @Test
  void existsByDateOrYearlyYearlyMatchTest() {
    persistHoliday(LocalDate.of(2025, 12, 25), true);
    em.flush();

    boolean exists2026 = repository.existsByDateOrYearly(LocalDate.of(2026, 12, 25), 12, 25);
    boolean exists2027 = repository.existsByDateOrYearly(LocalDate.of(2027, 12, 25), 12, 25);
    boolean notExists = repository.existsByDateOrYearly(LocalDate.of(2026, 12, 24), 12, 24);

    assertThat(exists2026).isTrue();
    assertThat(exists2027).isTrue();
    assertThat(notExists).isFalse();
  }

  @Test
  void findHolidayDatesBetweenTest() {
    persistHoliday(LocalDate.of(2026, 6, 1), false);
    persistHoliday(LocalDate.of(2026, 6, 15), false);
    persistHoliday(LocalDate.of(2026, 7, 1), false);
    persistHoliday(LocalDate.of(2025, 6, 10), true);
    em.flush();

    Set<LocalDate> dates = repository.findHolidayDatesBetween(
        LocalDate.of(2026, 6, 1),
        LocalDate.of(2026, 6, 30)
    );

    assertThat(dates).hasSize(3)
        .containsExactlyInAnyOrder(
            LocalDate.of(2026, 6, 1),
            LocalDate.of(2026, 6, 15),
            LocalDate.of(2025, 6, 10)
        );
  }

  @Test
  void deleteByIdDirectlyTest() {
    Holiday holiday = persistHoliday(LocalDate.of(2026, 1, 1), true);
    em.flush();

    repository.deleteByIdDirectly(holiday.getHolidayId());
    em.clear();

    Holiday found = em.find(Holiday.class, holiday.getHolidayId());
    assertThat(found).isNull();
  }

  private Holiday persistHoliday(LocalDate date, boolean yearly) {
    Holiday holiday = new Holiday();
    holiday.setName("Test Holiday");
    holiday.setDate(date);
    holiday.setOccursYearly(yearly);
    return em.persist(holiday);
  }
}
