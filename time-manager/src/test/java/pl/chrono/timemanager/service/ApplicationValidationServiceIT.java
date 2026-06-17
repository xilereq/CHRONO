package pl.chrono.timemanager.service;

import jakarta.persistence.EntityManager;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Set;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import pl.chrono.dbmanager.model.Application;
import pl.chrono.dbmanager.model.Policy;
import pl.chrono.dbmanager.model.Role;
import pl.chrono.dbmanager.model.User;
import pl.chrono.dbmanager.model.UsersPolicy;
import pl.chrono.dbmanager.model.enums.ApplicationStatus;
import pl.chrono.dbmanager.model.enums.ApplicationType;
import pl.chrono.dbmanager.repository.ApplicationRepository;
import pl.chrono.dbmanager.repository.PolicyRepository;
import pl.chrono.dbmanager.repository.RoleRepository;
import pl.chrono.dbmanager.repository.UserRepository;
import pl.chrono.dbmanager.repository.UsersPolicyRepository;
import pl.chrono.timemanager.TestConfig;
import pl.chrono.timemanager.exception.ApplicationValidationException;
import pl.chrono.timemanager.exception.UserException;

@SpringBootTest(classes = TestConfig.class)
@Transactional
class ApplicationValidationServiceIT {

  @Autowired
  private ApplicationValidationService applicationValidationService;

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private PolicyRepository policyRepository;

  @Autowired
  private UsersPolicyRepository usersPolicyRepository;

  @Autowired
  private ApplicationRepository applicationRepository;

  @Autowired
  private RoleRepository roleRepository;

  @Autowired
  private EntityManager entityManager;

  private User userWithPolicy;
  private User userWithoutPolicy;
  private Policy standardPolicy;

  private static final LocalDate MONDAY = LocalDate.of(2026, 6, 22);

  @BeforeEach
  void setUp() {
    standardPolicy = persistPolicy("Standard", 8, 12);
    userWithPolicy = persistUserWithPolicy("z.polityka@chrono.pl", standardPolicy);
    userWithoutPolicy = persistUser("bez.polityki@chrono.pl");
    entityManager.flush();
    entityManager.clear();
  }

  @Test
  void validate_throwsNotFound_whenUserDoesNotExist() {
    assertThatThrownBy(() ->
        applicationValidationService.validateApplicationLimits(
            999999, MONDAY, MONDAY, ApplicationType.HOLIDAY_LEAVE, null))
        .isInstanceOf(UserException.NotFound.class);
  }

  @Test
  void validate_holidayLeave_passesWhenSufficientBalance() {
    assertThatCode(() ->
        applicationValidationService.validateApplicationLimits(
            userWithPolicy.getUserId(), MONDAY, MONDAY, ApplicationType.HOLIDAY_LEAVE, null))
        .doesNotThrowAnyException();
  }

  @Test
  void validate_holidayLeave_passesForWeekend_returnsEarlyWithoutCheck() {
    LocalDate saturday = LocalDate.of(2026, 6, 20);
    assertThatCode(() ->
        applicationValidationService.validateApplicationLimits(
            userWithPolicy.getUserId(), saturday, saturday, ApplicationType.HOLIDAY_LEAVE, null))
        .doesNotThrowAnyException();
  }

  @Test
  void validate_holidayLeave_userWithoutPolicy_usesDefaultLimits() {
    assertThatCode(() ->
        applicationValidationService.validateApplicationLimits(
            userWithoutPolicy.getUserId(), MONDAY, MONDAY, ApplicationType.HOLIDAY_LEAVE, null))
        .doesNotThrowAnyException();
  }


  @Test
  void validate_overtimeEarn_passesWhenWithinDailyLimit() {
    int twoHours = 2 * 3600;
    assertThatCode(() ->
        applicationValidationService.validateApplicationLimits(
            userWithPolicy.getUserId(), MONDAY, MONDAY, ApplicationType.OVERTIME_EARN, twoHours))
        .doesNotThrowAnyException();
  }

  @Test
  void validate_overtimeEarn_throwsDailyLimitExceeded_whenOverMax() {
    int fiveHours = 5 * 3600;
    assertThatThrownBy(() ->
        applicationValidationService.validateApplicationLimits(
            userWithPolicy.getUserId(), MONDAY, MONDAY, ApplicationType.OVERTIME_EARN, fiveHours))
        .isInstanceOf(ApplicationValidationException.DailyLimitExceeded.class);
  }

  @Test
  void validate_overtimeEarn_passesForNullDuration() {
    assertThatCode(() ->
        applicationValidationService.validateApplicationLimits(
            userWithPolicy.getUserId(), MONDAY, MONDAY, ApplicationType.OVERTIME_EARN, null))
        .doesNotThrowAnyException();
  }

  @Test
  void validate_overtimeEarn_passesForZeroDuration() {
    assertThatCode(() ->
        applicationValidationService.validateApplicationLimits(
            userWithPolicy.getUserId(), MONDAY, MONDAY, ApplicationType.OVERTIME_EARN, 0))
        .doesNotThrowAnyException();
  }


  @Test
  void validate_earlyExit_passesWhenDurationWithinScheduled() {
    int twoHours = 2 * 3600;
    assertThatCode(() ->
        applicationValidationService.validateApplicationLimits(
            userWithPolicy.getUserId(), MONDAY, MONDAY, ApplicationType.EARLY_EXIT, twoHours))
        .doesNotThrowAnyException();
  }

  @Test
  void validate_earlyExit_throwsRequestedTimeExceedsScheduled_whenTooLong() {
    int tenHours = 10 * 3600;
    assertThatThrownBy(() ->
        applicationValidationService.validateApplicationLimits(
            userWithPolicy.getUserId(), MONDAY, MONDAY, ApplicationType.EARLY_EXIT, tenHours))
        .isInstanceOf(ApplicationValidationException.RequestedTimeExceedsScheduled.class);
  }

  @Test
  void validate_earlyExit_passesForNullDuration() {
    assertThatCode(() ->
        applicationValidationService.validateApplicationLimits(
            userWithPolicy.getUserId(), MONDAY, MONDAY, ApplicationType.EARLY_EXIT, null))
        .doesNotThrowAnyException();
  }

  @Test
  void validate_overtimeTake_passesWhenDurationWithinScheduled() {
    int fourHours = 4 * 3600;
    assertThatCode(() ->
        applicationValidationService.validateApplicationLimits(
            userWithPolicy.getUserId(), MONDAY, MONDAY, ApplicationType.OVERTIME_TAKE, fourHours))
        .doesNotThrowAnyException();
  }

  @Test
  void validate_overtimeTake_throwsRequestedTimeExceedsScheduled_whenTooLong() {
    int tenHours = 10 * 3600;
    assertThatThrownBy(() ->
        applicationValidationService.validateApplicationLimits(
            userWithPolicy.getUserId(), MONDAY, MONDAY, ApplicationType.OVERTIME_TAKE, tenHours))
        .isInstanceOf(ApplicationValidationException.RequestedTimeExceedsScheduled.class);
  }

  @Test
  void validate_homeOffice_passesWithoutValidation() {
    assertThatCode(() ->
        applicationValidationService.validateApplicationLimits(
            userWithPolicy.getUserId(), MONDAY, MONDAY, ApplicationType.HOME_OFFICE, null))
        .doesNotThrowAnyException();
  }

  @Test
  void validate_sickLeave_passesWithoutValidation() {
    assertThatCode(() ->
        applicationValidationService.validateApplicationLimits(
            userWithPolicy.getUserId(), MONDAY, MONDAY, ApplicationType.SICK_LEAVE, null))
        .doesNotThrowAnyException();
  }

  private Policy persistPolicy(String name, int dailyHours, int maxDailyHours) {
    Policy policy = new Policy();
    policy.setName(name);
    policy.setDailyHours(dailyHours);
    policy.setMaxDailyHours(maxDailyHours);
    policy.setWorkingDays(Set.of(DayOfWeek.MONDAY, DayOfWeek.TUESDAY,
        DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY));
    return policyRepository.save(policy);
  }

  private User persistUser(String email) {
    Role role = roleRepository.findByName("ROLE_EMPLOYEE")
        .orElseGet(() -> {
          Role r = new Role();
          r.setName("ROLE_EMPLOYEE");
          return roleRepository.save(r);
        });
    User user = new User();
    user.setName("Jan");
    user.setSurname("Kowalski");
    user.setEmail(email);
    user.setPasswordHash("hashed");
    user.setRoles(Set.of(role));
    user.setEmploymentStartDate(LocalDate.of(2020, 1, 1));
    return userRepository.save(user);
  }

  private User persistUserWithPolicy(String email, Policy policy) {
    User user = persistUser(email);

    UsersPolicy up = new UsersPolicy();
    up.setUser(user);
    up.setPolicy(policy);
    up.setValidFrom(LocalDate.of(2020, 1, 1));
    up.setValidTo(null);
    usersPolicyRepository.save(up);

    return user;
  }

  private void persistApprovedHolidayLeave(User user, LocalDate start, LocalDate end) {
    Application app = new Application();
    app.setEmployee(user);
    app.setType(ApplicationType.HOLIDAY_LEAVE);
    app.setStatus(ApplicationStatus.APPROVED);
    app.setStartDate(start);
    app.setEndDate(end);
    applicationRepository.save(app);
  }
}
