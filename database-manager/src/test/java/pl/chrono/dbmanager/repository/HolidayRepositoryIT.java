package pl.chrono.dbmanager.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
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
  void existsByDateExactMatchTest() {
    persistHoliday(LocalDate.of(2026, 5, 1));
    em.flush();

    boolean exists = repository.existsByDate(LocalDate.of(2026, 5, 1));
    boolean notExists = repository.existsByDate(LocalDate.of(2026, 5, 2));

    assertThat(exists).isTrue();
    assertThat(notExists).isFalse();
  }

  @Test
  void findAllByDateTest() {
    persistHoliday(LocalDate.of(2026, 12, 25));
    em.flush();

    List<Holiday> found = repository.findAllByDate(LocalDate.of(2026, 12, 25));
    List<Holiday> notFound = repository.findAllByDate(LocalDate.of(2026, 12, 24));

    assertThat(found).hasSize(1);
    assertThat(notFound).isEmpty();
  }

  @Test
  void findHolidayDatesBetweenTest() {
    persistHoliday(LocalDate.of(2026, 6, 1));
    persistHoliday(LocalDate.of(2026, 6, 15));
    persistHoliday(LocalDate.of(2026, 7, 1));
    em.flush();

    Set<LocalDate> dates = repository.findHolidayDatesBetween(
        LocalDate.of(2026, 6, 1),
        LocalDate.of(2026, 6, 30)
    );

    assertThat(dates).hasSize(2)
        .containsExactlyInAnyOrder(
            LocalDate.of(2026, 6, 1),
            LocalDate.of(2026, 6, 15)
        );
  }

  @Test
  void deleteByIdDirectlyTest() {
    Holiday holiday = persistHoliday(LocalDate.of(2026, 1, 1));
    em.flush();

    repository.deleteByIdDirectly(holiday.getHolidayId());
    em.clear();

    Holiday found = em.find(Holiday.class, holiday.getHolidayId());
    assertThat(found).isNull();
  }

  private Holiday persistHoliday(LocalDate date) {
    Holiday holiday = new Holiday();
    holiday.setName("Test Holiday");
    holiday.setDate(date);
    return em.persist(holiday);
  }
}
