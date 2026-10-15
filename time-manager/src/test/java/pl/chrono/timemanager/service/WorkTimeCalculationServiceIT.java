package pl.chrono.timemanager.service;

import jakarta.persistence.EntityManager;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Set;
import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import pl.chrono.dbmanager.model.Application;
import pl.chrono.dbmanager.model.Holiday;
import pl.chrono.dbmanager.model.HolidayPolicy;
import pl.chrono.dbmanager.model.Policy;
import pl.chrono.dbmanager.model.Role;
import pl.chrono.dbmanager.model.User;
import pl.chrono.dbmanager.model.enums.ApplicationStatus;
import pl.chrono.dbmanager.model.enums.ApplicationType;
import pl.chrono.dbmanager.repository.ApplicationRepository;
import pl.chrono.dbmanager.repository.HolidayPolicyRepository;
import pl.chrono.dbmanager.repository.HolidayRepository;
import pl.chrono.dbmanager.repository.PolicyRepository;
import pl.chrono.dbmanager.repository.RoleRepository;
import pl.chrono.dbmanager.repository.UserRepository;
import pl.chrono.timemanager.TestConfig;

@SpringBootTest(classes = TestConfig.class)
@Transactional
class WorkTimeCalculationServiceIT {

  @Autowired
  private WorkTimeCalculationService workTimeCalculationService;

  @Autowired
  private PolicyRepository policyRepository;

  @Autowired
  private HolidayRepository holidayRepository;

  @Autowired
  private HolidayPolicyRepository holidayPolicyRepository;

  @Autowired
  private ApplicationRepository applicationRepository;

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private RoleRepository roleRepository;

  @Autowired
  private EntityManager entityManager;

  private Policy standardPolicy;
  private User testUser;

  @BeforeEach
  void setUp() {
    standardPolicy = new Policy();
    standardPolicy.setName("Standard");
    standardPolicy.setDailyHours(8);
    standardPolicy.setMaxDailyHours(12);
    standardPolicy.setRoundingMinutes(0);
    standardPolicy.setWorkingDays(Set.of(
        DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
        DayOfWeek.THURSDAY, DayOfWeek.FRIDAY));
    policyRepository.save(standardPolicy);

    Role role = new Role();
    role.setName("ROLE_EMPLOYEE");
    roleRepository.save(role);

    testUser = new User();
    testUser.setEmail("test@chrono.pl");
    testUser.setName("Jan");
    testUser.setSurname("Kowalski");
    testUser.setRoles(Set.of(role));
    testUser.setPasswordHash("123");
    userRepository.save(testUser);
  }


  @Test
  void calculateWorkingDays_fullWeek_returnsFive() {
    int result = workTimeCalculationService.calculateWorkingDays(
        LocalDate.of(2026, 6, 15),
        LocalDate.of(2026, 6, 19),
        standardPolicy);

    assertThat(result).isEqualTo(5);
  }

  @Test
  void calculateWorkingDays_weekendOnly_returnsZero() {
    int result = workTimeCalculationService.calculateWorkingDays(
        LocalDate.of(2026, 6, 20),
        LocalDate.of(2026, 6, 21),
        standardPolicy);

    assertThat(result).isEqualTo(0);
  }

  @Test
  void calculateWorkingDays_withHolidayInRange_excludesHoliday() {
    Holiday holiday = persistHoliday("Boze Narodzenie", LocalDate.of(2026, 12, 25));
    persistHolidayPolicyLink(holiday, standardPolicy);
    entityManager.flush();
    entityManager.clear();

    Policy refreshed = policyRepository.findById(standardPolicy.getPolicyId()).orElseThrow();

    int result = workTimeCalculationService.calculateWorkingDays(
        LocalDate.of(2026, 12, 21),
        LocalDate.of(2026, 12, 25),
        refreshed);

    assertThat(result).isEqualTo(4);
  }

  @Test
  void calculateWorkingDays_startAfterEnd_returnsZero() {
    int result = workTimeCalculationService.calculateWorkingDays(
        LocalDate.of(2026, 6, 19),
        LocalDate.of(2026, 6, 15),
        standardPolicy);

    assertThat(result).isEqualTo(0);
  }

  @Test
  void calculateWorkingDays_nullDates_returnsZero() {
    int result = workTimeCalculationService.calculateWorkingDays(null, null, standardPolicy);
    assertThat(result).isEqualTo(0);
  }

  @Test
  void calculateWorkingDays_singleWorkingDay_returnsOne() {
    LocalDate monday = LocalDate.of(2026, 6, 15);
    int result = workTimeCalculationService.calculateWorkingDays(monday, monday, standardPolicy);
    assertThat(result).isEqualTo(1);
  }

  @Test
  void getBaseRequiredSeconds_workingDay_returns8Hours() {
    LocalDate monday = LocalDate.of(2026, 6, 15);
    int result = workTimeCalculationService.getBaseRequiredSeconds(monday, standardPolicy);
    assertThat(result).isEqualTo(8 * 3600);
  }

  @Test
  void getBaseRequiredSeconds_saturday_returnsZero() {
    LocalDate saturday = LocalDate.of(2026, 6, 20);
    int result = workTimeCalculationService.getBaseRequiredSeconds(saturday, standardPolicy);
    assertThat(result).isEqualTo(0);
  }

  @Test
  void getBaseRequiredSeconds_nullPolicy_weekday_returns8HoursDefault() {
    LocalDate monday = LocalDate.of(2026, 6, 15);
    int result = workTimeCalculationService.getBaseRequiredSeconds(monday, null);
    assertThat(result).isEqualTo(8 * 3600);
  }

  @Test
  void getBaseRequiredSeconds_nullPolicy_saturday_returnsZero() {
    LocalDate saturday = LocalDate.of(2026, 6, 20);
    int result = workTimeCalculationService.getBaseRequiredSeconds(saturday, null);
    assertThat(result).isEqualTo(0);
  }

  @Test
  void getAdjustedRequiredSeconds_noApplications_returnsBase() {
    LocalDate monday = LocalDate.of(2026, 6, 15);
    int result = workTimeCalculationService.getAdjustedRequiredSeconds(
        testUser.getUserId(), monday, standardPolicy);
    assertThat(result).isEqualTo(8 * 3600);
  }

  @Test
  void getAdjustedRequiredSeconds_earlyExitApplication_reducesRequired() {
    LocalDate monday = LocalDate.of(2026, 6, 15);
    persistApplication(testUser, ApplicationType.EARLY_EXIT,
        ApplicationStatus.APPROVED, monday, monday, 2 * 3600);

    int result = workTimeCalculationService.getAdjustedRequiredSeconds(
        testUser.getUserId(), monday, standardPolicy);

    assertThat(result).isEqualTo(6 * 3600);
  }

  @Test
  void getAdjustedRequiredSeconds_overtimeTake_reducesRequired() {
    LocalDate monday = LocalDate.of(2026, 6, 15);
    persistApplication(testUser, ApplicationType.OVERTIME_TAKE,
        ApplicationStatus.APPROVED, monday, monday, 3600);

    int result = workTimeCalculationService.getAdjustedRequiredSeconds(
        testUser.getUserId(), monday, standardPolicy);

    assertThat(result).isEqualTo(7 * 3600);
  }

  @Test
  void getAdjustedRequiredSeconds_overtimeEarn_increasesRequired() {
    LocalDate monday = LocalDate.of(2026, 6, 15);
    persistApplication(testUser, ApplicationType.OVERTIME_EARN,
        ApplicationStatus.APPROVED, monday, monday, 2 * 3600);

    int result = workTimeCalculationService.getAdjustedRequiredSeconds(
        testUser.getUserId(), monday, standardPolicy);

    assertThat(result).isEqualTo(10 * 3600);
  }

  @Test
  void getAdjustedRequiredSeconds_pendingApplication_doesNotAffect() {
    LocalDate monday = LocalDate.of(2026, 6, 15);
    persistApplication(testUser, ApplicationType.EARLY_EXIT,
        ApplicationStatus.PENDING, monday, monday, 2 * 3600);

    int result = workTimeCalculationService.getAdjustedRequiredSeconds(
        testUser.getUserId(), monday, standardPolicy);

    assertThat(result).isEqualTo(8 * 3600);
  }

  @Test
  void getAdjustedRequiredSeconds_cannotGoBelowZero() {
    LocalDate monday = LocalDate.of(2026, 6, 15);
    persistApplication(testUser, ApplicationType.EARLY_EXIT,
        ApplicationStatus.APPROVED, monday, monday, 10 * 3600);

    int result = workTimeCalculationService.getAdjustedRequiredSeconds(
        testUser.getUserId(), monday, standardPolicy);

    assertThat(result).isEqualTo(0);
  }

  @Test
  void hasApprovedHomeOffice_withApprovedApplication_returnsTrue() {
    LocalDate monday = LocalDate.of(2026, 6, 15);
    persistApplication(testUser, ApplicationType.HOME_OFFICE,
        ApplicationStatus.APPROVED, monday, monday, null);

    boolean result = workTimeCalculationService.hasApprovedHomeOffice(
        testUser.getUserId(), monday);

    assertThat(result).isTrue();
  }

  @Test
  void hasApprovedHomeOffice_withPendingApplication_returnsFalse() {
    LocalDate monday = LocalDate.of(2026, 6, 15);
    persistApplication(testUser, ApplicationType.HOME_OFFICE,
        ApplicationStatus.PENDING, monday, monday, null);

    boolean result = workTimeCalculationService.hasApprovedHomeOffice(
        testUser.getUserId(), monday);

    assertThat(result).isFalse();
  }

  @Test
  void hasApprovedHomeOffice_noApplication_returnsFalse() {
    LocalDate monday = LocalDate.of(2026, 6, 15);
    boolean result = workTimeCalculationService.hasApprovedHomeOffice(
        testUser.getUserId(), monday);
    assertThat(result).isFalse();
  }

  @Test
  void formatSecondsToTime_fullHour_returnsFormattedString() {
    assertThat(workTimeCalculationService.formatSecondsToTime(3600)).isEqualTo("01:00:00");
  }

  @Test
  void formatSecondsToTime_zero_returnsZero() {
    assertThat(workTimeCalculationService.formatSecondsToTime(0)).isEqualTo("00:00:00");
  }

  @Test
  void formatSecondsToTime_mixedHoursMinutesSeconds_returnsFormatted() {
    assertThat(workTimeCalculationService.formatSecondsToTime(3661)).isEqualTo("01:01:01");
  }

  @Test
  void formatSecondsToTime_8Hours_returnsEightHours() {
    assertThat(workTimeCalculationService.formatSecondsToTime(8 * 3600)).isEqualTo("08:00:00");
  }

  @Test
  void getMaxAllowedSeconds_withPolicy_returnsFromPolicy() {
    assertThat(workTimeCalculationService.getMaxAllowedSeconds(standardPolicy))
        .isEqualTo(12 * 3600);
  }

  @Test
  void getMaxAllowedSeconds_nullPolicy_returns12HoursDefault() {
    assertThat(workTimeCalculationService.getMaxAllowedSeconds(null))
        .isEqualTo(12 * 3600);
  }

  @Test
  void getRoundingSeconds_withPolicy_returnsRoundingInSeconds() {
    standardPolicy.setRoundingMinutes(15);
    assertThat(workTimeCalculationService.getRoundingSeconds(standardPolicy))
        .isEqualTo(15 * 60);
  }

  @Test
  void getRoundingSeconds_nullPolicy_returnsZero() {
    assertThat(workTimeCalculationService.getRoundingSeconds(null)).isEqualTo(0);
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

  private void persistApplication(User user, ApplicationType type,
                                  ApplicationStatus status, LocalDate start, LocalDate end, Integer durationSeconds) {
    Application app = new Application();
    app.setEmployee(user);
    app.setType(type);
    app.setStatus(status);
    app.setStartDate(start);
    app.setEndDate(end);
    app.setDurationSeconds(durationSeconds);
    applicationRepository.save(app);
  }
}
