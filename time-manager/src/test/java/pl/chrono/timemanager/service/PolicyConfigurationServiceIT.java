package pl.chrono.timemanager.service;

import jakarta.persistence.EntityManager;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.BeforeEach;
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
import pl.chrono.timemanager.dto.request.AssignHolidayRequest;
import pl.chrono.timemanager.dto.request.CreatePolicyRequest;
import pl.chrono.timemanager.dto.request.MassAssignHolidayRequest;
import pl.chrono.timemanager.dto.response.HolidayDto;
import pl.chrono.timemanager.dto.response.PolicyDto;
import pl.chrono.timemanager.exception.HolidayException;
import pl.chrono.timemanager.exception.PolicyException;

@SpringBootTest(classes = TestConfig.class)
@Transactional
class PolicyConfigurationServiceIT {

  @Autowired
  private PolicyConfigurationService policyConfigurationService;

  @Autowired
  private PolicyRepository policyRepository;

  @Autowired
  private HolidayRepository holidayRepository;

  @Autowired
  private HolidayPolicyRepository holidayPolicyRepository;

  @Autowired
  private EntityManager entityManager;

  private Policy savedPolicy;
  private Holiday savedHoliday;

  @BeforeEach
  void setUp() {
    savedPolicy = persistPolicy("Standard", 8);
    savedHoliday = persistHoliday("Nowy Rok", LocalDate.of(2026, 1, 1));
  }

  @Test
  void createPolicy_savesAndReturnsCorrectFields() {
    CreatePolicyRequest request = new CreatePolicyRequest(
        "Elastyczna", 7, 12, 48, 15,
        List.of("MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY")
    );

    PolicyDto result = policyConfigurationService.createPolicy(request);

    assertThat(result.id()).isNotNull();
    assertThat(result.name()).isEqualTo("Elastyczna");
    assertThat(result.dailyHours()).isEqualTo(7);
    assertThat(policyRepository.findById(result.id())).isPresent();
  }

  @Test
  void createPolicy_withWorkingDays_persistsWorkingDays() {
    CreatePolicyRequest request = new CreatePolicyRequest(
        "Weekendowa", 8, 12, 48, 0,
        List.of("SATURDAY", "SUNDAY")
    );

    PolicyDto result = policyConfigurationService.createPolicy(request);

    Policy saved = policyRepository.findById(result.id()).orElseThrow();
    assertThat(saved.getWorkingDays()).containsExactlyInAnyOrder(
        DayOfWeek.SATURDAY, DayOfWeek.SUNDAY);
  }

  @Test
  void getAllPolicies_returnsAllPersisted() {
    persistPolicy("Dodatkowa", 6);

    List<PolicyDto> result = policyConfigurationService.getAllPolicies();

    assertThat(result).hasSize(2);
    assertThat(result).extracting(PolicyDto::name)
        .containsExactlyInAnyOrder("Standard", "Dodatkowa");
  }

  @Test
  void getAllPolicies_emptyDatabase_returnsEmptyList() {
    policyRepository.deleteAll();

    List<PolicyDto> result = policyConfigurationService.getAllPolicies();

    assertThat(result).isEmpty();
  }

  @Test
  void getPolicyById_returnsMatchingPolicy() {
    PolicyDto result = policyConfigurationService.getPolicyById(savedPolicy.getPolicyId());

    assertThat(result.name()).isEqualTo("Standard");
    assertThat(result.dailyHours()).isEqualTo(8);
  }

  @Test
  void getPolicyById_throwsNotFound_whenDoesNotExist() {
    assertThatThrownBy(() -> policyConfigurationService.getPolicyById(999999))
        .isInstanceOf(PolicyException.NotFound.class);
  }

  @Test
  void deletePolicy_removesPolicyFromDatabase() {
    policyConfigurationService.deletePolicy(savedPolicy.getPolicyId());

    entityManager.flush();
    entityManager.clear();

    assertThat(policyRepository.findById(savedPolicy.getPolicyId())).isEmpty();
  }

  @Test
  void deletePolicy_removesHolidayPolicyAssociations() {
    persistHolidayPolicyLink(savedHoliday, savedPolicy);
    entityManager.flush();

    policyConfigurationService.deletePolicy(savedPolicy.getPolicyId());

    entityManager.flush();
    entityManager.clear();

    assertThat(holidayPolicyRepository.findByPolicy(savedPolicy)).isEmpty();
  }

  @Test
  void deletePolicy_throwsNotFound_whenDoesNotExist() {
    assertThatThrownBy(() -> policyConfigurationService.deletePolicy(999999))
        .isInstanceOf(PolicyException.NotFound.class);
  }

  @Test
  void getHolidaysForPolicy_returnsAssignedHolidays() {
    persistHolidayPolicyLink(savedHoliday, savedPolicy);
    entityManager.flush();
    entityManager.clear();

    List<HolidayDto> result = policyConfigurationService
        .getHolidaysForPolicy(savedPolicy.getPolicyId());

    assertThat(result).hasSize(1);
    assertThat(result.getFirst().name()).isEqualTo("Nowy Rok");
  }

  @Test
  void getHolidaysForPolicy_noHolidays_returnsEmptyList() {
    List<HolidayDto> result = policyConfigurationService
        .getHolidaysForPolicy(savedPolicy.getPolicyId());

    assertThat(result).isEmpty();
  }

  @Test
  void getHolidaysForPolicy_throwsNotFound_whenPolicyDoesNotExist() {
    assertThatThrownBy(() -> policyConfigurationService.getHolidaysForPolicy(999999))
        .isInstanceOf(PolicyException.NotFound.class);
  }

  @Test
  void assignHolidayToPolicy_createsLink() {
    AssignHolidayRequest request = new AssignHolidayRequest(savedHoliday.getHolidayId());

    policyConfigurationService.assignHolidayToPolicy(savedPolicy.getPolicyId(), request);

    entityManager.flush();
    entityManager.clear();

    assertThat(holidayPolicyRepository.existsByPolicyAndHoliday(savedPolicy, savedHoliday))
        .isTrue();
  }

  @Test
  void assignHolidayToPolicy_throwsAlreadyAssigned_whenDuplicate() {
    persistHolidayPolicyLink(savedHoliday, savedPolicy);
    entityManager.flush();

    AssignHolidayRequest request = new AssignHolidayRequest(savedHoliday.getHolidayId());

    assertThatThrownBy(() ->
        policyConfigurationService.assignHolidayToPolicy(savedPolicy.getPolicyId(), request))
        .isInstanceOf(HolidayException.AlreadyAssigned.class);
  }

  @Test
  void assignHolidayToPolicy_throwsNotFound_whenPolicyMissing() {
    AssignHolidayRequest request = new AssignHolidayRequest(savedHoliday.getHolidayId());

    assertThatThrownBy(() ->
        policyConfigurationService.assignHolidayToPolicy(999999, request))
        .isInstanceOf(PolicyException.NotFound.class);
  }

  @Test
  void assignHolidayToPolicy_throwsNotFound_whenHolidayMissing() {
    AssignHolidayRequest request = new AssignHolidayRequest(999999);

    assertThatThrownBy(() ->
        policyConfigurationService.assignHolidayToPolicy(savedPolicy.getPolicyId(), request))
        .isInstanceOf(HolidayException.NotFound.class);
  }

  @Test
  void removeHolidayFromPolicy_removesLink() {
    persistHolidayPolicyLink(savedHoliday, savedPolicy);
    entityManager.flush();

    policyConfigurationService.removeHolidayFromPolicy(
        savedPolicy.getPolicyId(), savedHoliday.getHolidayId());

    entityManager.flush();
    entityManager.clear();

    assertThat(holidayPolicyRepository.existsByPolicyAndHoliday(savedPolicy, savedHoliday))
        .isFalse();
  }

  @Test
  void removeHolidayFromPolicy_throwsNotAssigned_whenLinkMissing() {
    assertThatThrownBy(() ->
        policyConfigurationService.removeHolidayFromPolicy(
            savedPolicy.getPolicyId(), savedHoliday.getHolidayId()))
        .isInstanceOf(HolidayException.NotAssigned.class);
  }

  @Test
  void assignHolidaysToPolicyBulk_assignsAllHolidays() {
    Holiday second = persistHoliday("Boze Narodzenie", LocalDate.of(2026, 12, 25));
    MassAssignHolidayRequest request = new MassAssignHolidayRequest(
        List.of(savedHoliday.getHolidayId(), second.getHolidayId()));

    policyConfigurationService.assignHolidaysToPolicyBulk(savedPolicy.getPolicyId(), request);

    entityManager.flush();
    entityManager.clear();

    List<HolidayPolicy> links = holidayPolicyRepository.findByPolicy(savedPolicy);
    assertThat(links).hasSize(2);
  }

  @Test
  void assignHolidaysToPolicyBulk_skipsAlreadyAssigned() {
    persistHolidayPolicyLink(savedHoliday, savedPolicy);
    Holiday second = persistHoliday("Boze Narodzenie", LocalDate.of(2026, 12, 25));
    entityManager.flush();

    MassAssignHolidayRequest request = new MassAssignHolidayRequest(
        List.of(savedHoliday.getHolidayId(), second.getHolidayId()));

    policyConfigurationService.assignHolidaysToPolicyBulk(savedPolicy.getPolicyId(), request);

    entityManager.flush();
    entityManager.clear();

    List<HolidayPolicy> links = holidayPolicyRepository.findByPolicy(savedPolicy);
    assertThat(links).hasSize(2);
  }

  @Test
  void assignHolidaysToPolicyBulk_throwsNoneFound_whenNoHolidaysExist() {
    MassAssignHolidayRequest request = new MassAssignHolidayRequest(List.of(999998, 999999));

    assertThatThrownBy(() ->
        policyConfigurationService.assignHolidaysToPolicyBulk(savedPolicy.getPolicyId(), request))
        .isInstanceOf(HolidayException.NoneFound.class);
  }

  private Policy persistPolicy(String name, int dailyHours) {
    Policy policy = new Policy();
    policy.setName(name);
    policy.setDailyHours(dailyHours);
    policy.setWorkingDays(Set.of(DayOfWeek.MONDAY, DayOfWeek.TUESDAY,
        DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY));
    return policyRepository.save(policy);
  }

  private Holiday persistHoliday(String name, LocalDate date) {
    Holiday h = new Holiday();
    h.setName(name);
    h.setDate(date);
    return holidayRepository.save(h);
  }

  private void persistHolidayPolicyLink(Holiday holiday, Policy policy) {
    HolidayPolicy link = new HolidayPolicy();
    link.setHoliday(holiday);
    link.setPolicy(policy);
    holidayPolicyRepository.save(link);
  }
}
