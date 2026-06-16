package pl.chrono.dbmanager.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import pl.chrono.dbmanager.model.Holiday;
import pl.chrono.dbmanager.model.HolidayPolicy;
import pl.chrono.dbmanager.model.Policy;

@DataJpaTest
class HolidayPolicyRepositoryIT {

  @Autowired
  private TestEntityManager em;

  @Autowired
  private HolidayPolicyRepository repository;

  private Policy policy;
  private Holiday holiday;

  @BeforeEach
  void setUp() {
    policy = new Policy();
    policy.setName("Test Policy");
    policy.setDailyHours(8);
    em.persist(policy);

    holiday = new Holiday();
    holiday.setName("Test Holiday");
    holiday.setDate(LocalDate.now());
    em.persist(holiday);

    em.flush();
  }

  @Test
  void findByPolicyAndHolidayTest() {
    HolidayPolicy hp = persistHolidayPolicy(policy, holiday);

    Optional<HolidayPolicy> result = repository.findByPolicyAndHoliday(policy, holiday);

    assertThat(result).isPresent().get().isEqualTo(hp);
  }

  @Test
  void existsByPolicyAndHolidayTest() {
    persistHolidayPolicy(policy, holiday);

    Holiday anotherHoliday = new Holiday();
    anotherHoliday.setName("Another Holiday");
    anotherHoliday.setDate(LocalDate.now().plusDays(1));
    anotherHoliday.setOccursYearly(false);
    em.persist(anotherHoliday);
    em.flush();

    boolean exists = repository.existsByPolicyAndHoliday(policy, holiday);

    boolean notExists = repository.existsByPolicyAndHoliday(policy, anotherHoliday);

    assertThat(exists).isTrue();
    assertThat(notExists).isFalse();
  }

  @Test
  void deleteByHolidayIdTest() {
    persistHolidayPolicy(policy, holiday);
    em.flush();

    repository.deleteByHolidayId(holiday.getHolidayId());
    em.clear();

    List<HolidayPolicy> result = repository.findByPolicy(policy);
    assertThat(result).isEmpty();
  }

  @Test
  void deleteByPolicyTest() {
    persistHolidayPolicy(policy, holiday);
    em.flush();

    repository.deleteByPolicy(policy);
    em.flush();
    em.clear();

    List<HolidayPolicy> result = repository.findAll();
    assertThat(result).isEmpty();
  }

  private HolidayPolicy persistHolidayPolicy(Policy p, Holiday h) {
    HolidayPolicy hp = new HolidayPolicy();
    hp.setPolicy(p);
    hp.setHoliday(h);
    return em.persistAndFlush(hp);
  }
}
