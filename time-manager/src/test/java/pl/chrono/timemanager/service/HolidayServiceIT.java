package pl.chrono.timemanager.service;

import java.time.LocalDate;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import pl.chrono.dbmanager.model.Holiday;
import pl.chrono.dbmanager.model.HolidayPolicy;
import pl.chrono.dbmanager.model.Policy;
import pl.chrono.dbmanager.repository.HolidayPolicyRepository;
import pl.chrono.dbmanager.repository.HolidayRepository;
import pl.chrono.dbmanager.repository.PolicyRepository;
import pl.chrono.timemanager.TestConfig;
import pl.chrono.timemanager.dto.request.CreateHolidayRequest;
import pl.chrono.timemanager.dto.response.HolidayDto;
import pl.chrono.timemanager.exception.HolidayException;

@SpringBootTest(classes = TestConfig.class)
@Transactional
class HolidayServiceIT {

  @Autowired
  private HolidayService holidayService;

  @Autowired
  private HolidayRepository holidayRepository;

  @Autowired
  private HolidayPolicyRepository holidayPolicyRepository;

  @Autowired
  private PolicyRepository policyRepository;

  @Autowired
  private EntityManager entityManager;

  @Test
  void createHoliday_savesAndReturnsHolidayWithCorrectFields() {
    CreateHolidayRequest request = new CreateHolidayRequest(
        "Boze Narodzenie", true, LocalDate.of(2026, 12, 25));

    HolidayDto result = holidayService.createHoliday(request);

    assertThat(result.holidayId()).isNotNull();
    assertThat(result.name()).isEqualTo("Boze Narodzenie");
    assertThat(result.occursYearly()).isTrue();
    assertThat(result.date()).isEqualTo(LocalDate.of(2026, 12, 25));
    assertThat(holidayRepository.findById(result.holidayId())).isPresent();
  }

  @Test
  void getAllHolidays_returnsAllPersistedHolidays() {
    persistHoliday("Nowy Rok", true, LocalDate.of(2026, 1, 1));
    persistHoliday("Konstytucja 3 Maja", true, LocalDate.of(2026, 5, 3));

    List<HolidayDto> result = holidayService.getAllHolidays();

    assertThat(result).hasSize(2);
    assertThat(result).extracting(HolidayDto::name)
        .containsExactlyInAnyOrder("Nowy Rok", "Konstytucja 3 Maja");
  }

  @Test
  void getHolidayById_returnsMatchingHoliday() {
    Holiday saved = persistHoliday("Wszystkich Swietych", true, LocalDate.of(2026, 11, 1));

    HolidayDto result = holidayService.getHolidayById(saved.getHolidayId());

    assertThat(result.name()).isEqualTo("Wszystkich Swietych");
  }

  @Test
  void getHolidayById_throwsNotFound_whenDoesNotExist() {
    assertThatThrownBy(() -> holidayService.getHolidayById(999999))
        .isInstanceOf(HolidayException.NotFound.class);
  }

  @Test
  void deleteHoliday_removesHolidayAndItsPolicyAssociations() {
    Holiday holiday = persistHoliday("Swieto Pracy", true, LocalDate.of(2026, 5, 1));
    Policy policy = persistPolicy();
    persistHolidayPolicyLink(holiday, policy);

    entityManager.flush();

    holidayService.deleteHoliday(holiday.getHolidayId());

    entityManager.flush();
    entityManager.clear();

    assertThat(holidayRepository.findById(holiday.getHolidayId())).isEmpty();
    assertThat(holidayPolicyRepository.findByPolicy(policy)).isEmpty();
  }

  @Test
  void isHoliday_trueForExactDateMatch_whenNotRecurring() {
    persistHoliday("Jednorazowe swieto firmowe", false, LocalDate.of(2026, 6, 17));

    assertThat(holidayService.isHoliday(LocalDate.of(2026, 6, 17))).isTrue();
  }

  @Test
  void isHoliday_falseForSameMonthDayDifferentYear_whenNotRecurring() {
    persistHoliday("Jednorazowe swieto firmowe", false, LocalDate.of(2026, 6, 17));

    assertThat(holidayService.isHoliday(LocalDate.of(2027, 6, 17))).isFalse();
  }

  @Test
  void isHoliday_trueForSameMonthDayDifferentYear_whenRecurringYearly() {
    persistHoliday("Boze Narodzenie", true, LocalDate.of(2020, 12, 25));

    assertThat(holidayService.isHoliday(LocalDate.of(2026, 12, 25))).isTrue();
  }

  @Test
  void isHoliday_falseWhenNoMatchingHolidayExists() {
    persistHoliday("Boze Narodzenie", true, LocalDate.of(2020, 12, 25));

    assertThat(holidayService.isHoliday(LocalDate.of(2026, 7, 1))).isFalse();
  }

  private Holiday persistHoliday(String name, boolean occursYearly, LocalDate date) {
    Holiday holiday = new Holiday();
    holiday.setName(name);
    holiday.setOccursYearly(occursYearly);
    holiday.setDate(date);
    return holidayRepository.save(holiday);
  }

  private Policy persistPolicy() {
    Policy policy = new Policy();
    policy.setName("Standard");
    policy.setDailyHours(8);
    return policyRepository.save(policy);
  }

  private void persistHolidayPolicyLink(Holiday holiday, Policy policy) {
    HolidayPolicy link = new HolidayPolicy();
    link.setHoliday(holiday);
    link.setPolicy(policy);
    holidayPolicyRepository.save(link);
  }
}
