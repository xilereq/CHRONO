package pl.chrono.timemanager.service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
import pl.chrono.timemanager.TestConfig;
import pl.chrono.timemanager.exception.UserException;

@SpringBootTest(classes = TestConfig.class)
@Transactional
class LeaveBalanceServiceIT {

  @Autowired
  private LeaveBalanceService leaveBalanceService;

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private ApplicationRepository applicationRepository;

  private User user;

  @BeforeEach
  void setUp() {
    user = userRepository.save(buildUser("Jan", "Kowalski", "jan.kowalski@test.com"));
  }

  @Test
  void calculateAnnualLeaveLimit_returnsBaseLimit_whenNoEmploymentStartDate() {
    int limit = leaveBalanceService.calculateAnnualLeaveLimit(user, 2026);

    assertThat(limit).isEqualTo(26);
  }

  @Test
  void calculateAnnualLeaveLimit_addsOneDayPerYearOfService() {
    user.setEmploymentStartDate(LocalDate.of(2023, 1, 1));
    userRepository.save(user);

    int limit = leaveBalanceService.calculateAnnualLeaveLimit(user, 2026);

    assertThat(limit).isEqualTo(26 + 3);
  }

  @Test
  void calculateAnnualLeaveLimit_capsAtMaximumThirtyOneDays() {
    user.setEmploymentStartDate(LocalDate.of(2000, 1, 1));
    userRepository.save(user);

    int limit = leaveBalanceService.calculateAnnualLeaveLimit(user, 2026);

    assertThat(limit).isEqualTo(31);
  }

  @Test
  void calculateAnnualLeaveLimit_noArgOverload_usesCurrentYear() {
    user.setEmploymentStartDate(LocalDate.of(2023, 1, 1));
    userRepository.save(user);

    int limitWithCurrentYear = leaveBalanceService.calculateAnnualLeaveLimit(
        user, LocalDate.now().getYear());
    int limitFromNoArgOverload = leaveBalanceService.calculateAnnualLeaveLimit(user);

    assertThat(limitFromNoArgOverload).isEqualTo(limitWithCurrentYear);
  }

  @Test
  void getUsedLeaveDaysInYear_sumsWorkingDaysFromApprovedHolidayLeave() {
    LocalDate monday = mondayOnOrAfter(LocalDate.of(2026, 6, 1));
    LocalDate friday = monday.plusDays(4);
    persistApplication(ApplicationType.HOLIDAY_LEAVE, ApplicationStatus.APPROVED,
        monday, friday);

    int used = leaveBalanceService.getUsedLeaveDaysInYear(user.getUserId(), 2026);

    assertThat(used).isEqualTo(5);
  }

  @Test
  void getUsedLeaveDaysInYear_excludesWeekendDaysFromTheRange() {
    LocalDate monday = mondayOnOrAfter(LocalDate.of(2026, 6, 1));
    LocalDate friday = monday.plusDays(4);
    LocalDate followingMonday = friday.plusDays(3);
    persistApplication(ApplicationType.HOLIDAY_LEAVE, ApplicationStatus.APPROVED,
        friday, followingMonday);

    int used = leaveBalanceService.getUsedLeaveDaysInYear(user.getUserId(), 2026);

    assertThat(used).isEqualTo(2);
  }

  @Test
  void getUsedLeaveDaysInYear_ignoresNonApprovedApplications() {
    persistApplication(ApplicationType.HOLIDAY_LEAVE, ApplicationStatus.PENDING,
        LocalDate.of(2026, 6, 15), LocalDate.of(2026, 6, 19));

    int used = leaveBalanceService.getUsedLeaveDaysInYear(user.getUserId(), 2026);

    assertThat(used).isEqualTo(0);
  }

  @Test
  void getUsedLeaveDaysInYear_ignoresOtherApplicationTypes() {
    persistApplication(ApplicationType.SICK_LEAVE, ApplicationStatus.APPROVED,
        LocalDate.of(2026, 6, 15), LocalDate.of(2026, 6, 19));

    int used = leaveBalanceService.getUsedLeaveDaysInYear(user.getUserId(), 2026);

    assertThat(used).isEqualTo(0);
  }

  @Test
  void getUsedLeaveDaysInYear_ignoresApplicationsFromOtherYears() {
    persistApplication(ApplicationType.HOLIDAY_LEAVE, ApplicationStatus.APPROVED,
        LocalDate.of(2025, 6, 15), LocalDate.of(2025, 6, 19));

    int used = leaveBalanceService.getUsedLeaveDaysInYear(user.getUserId(), 2026);

    assertThat(used).isEqualTo(0);
  }

  @Test
  void getUsedLeaveDaysInYear_sumsAcrossMultipleApprovedApplications() {
    LocalDate mondayJanuary = mondayOnOrAfter(LocalDate.of(2026, 1, 5));
    LocalDate mondayJune = mondayOnOrAfter(LocalDate.of(2026, 6, 1));
    persistApplication(ApplicationType.HOLIDAY_LEAVE, ApplicationStatus.APPROVED,
        mondayJanuary, mondayJanuary.plusDays(4));
    persistApplication(ApplicationType.HOLIDAY_LEAVE, ApplicationStatus.APPROVED,
        mondayJune, mondayJune.plusDays(4));

    int used = leaveBalanceService.getUsedLeaveDaysInYear(user.getUserId(), 2026);

    assertThat(used).isEqualTo(5 + 5);
  }

  @Test
  void getUsedLeaveDaysInYear_throwsUserNotFound_whenEmployeeDoesNotExist() {
    assertThatThrownBy(() -> leaveBalanceService.getUsedLeaveDaysInYear(999999, 2026))
        .isInstanceOf(UserException.NotFound.class);
  }

  @Test
  void getRolloverDaysFromPreviousYear_returnsRemainingDaysFromPreviousYear() {
    LocalDate monday2025 = mondayOnOrAfter(LocalDate.of(2025, 6, 1));
    persistApplication(ApplicationType.HOLIDAY_LEAVE, ApplicationStatus.APPROVED,
        monday2025, monday2025.plusDays(4));

    int rollover = leaveBalanceService.getRolloverDaysFromPreviousYear(user.getUserId(), 2026);

    assertThat(rollover).isEqualTo(26 - 5);
  }

  @Test
  void getRolloverDaysFromPreviousYear_neverReturnsNegative() {
    LocalDate monday2025 = mondayOnOrAfter(LocalDate.of(2025, 1, 1));
    persistApplication(ApplicationType.HOLIDAY_LEAVE, ApplicationStatus.APPROVED,
        monday2025, monday2025.plusWeeks(6).minusDays(3));

    int rollover = leaveBalanceService.getRolloverDaysFromPreviousYear(user.getUserId(), 2026);

    assertThat(rollover).isEqualTo(0);
  }

  private LocalDate mondayOnOrAfter(LocalDate date) {
    return date.with(TemporalAdjusters.nextOrSame(DayOfWeek.MONDAY));
  }

  private User buildUser(String name, String surname, String email) {
    User newUser = new User();
    newUser.setName(name);
    newUser.setSurname(surname);
    newUser.setEmail(email);
    newUser.setPasswordHash("hash");
    return newUser;
  }

  private Application persistApplication(ApplicationType type, ApplicationStatus status,
                                         LocalDate startDate, LocalDate endDate) {
    Application application = new Application();
    application.setEmployee(user);
    application.setType(type);
    application.setStatus(status);
    application.setStartDate(startDate);
    application.setEndDate(endDate);
    return applicationRepository.save(application);
  }
}
