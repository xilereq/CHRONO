package pl.chrono.timemanager.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashSet;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import pl.chrono.dbmanager.model.Policy;
import pl.chrono.dbmanager.model.User;
import pl.chrono.dbmanager.model.UsersPolicy;
import pl.chrono.dbmanager.model.WorkLog;
import pl.chrono.dbmanager.model.enums.WorkLocationType;
import pl.chrono.dbmanager.repository.PolicyRepository;
import pl.chrono.dbmanager.repository.UserRepository;
import pl.chrono.dbmanager.repository.UsersPolicyRepository;
import pl.chrono.dbmanager.repository.WorkLogRepository;
import pl.chrono.timemanager.TestConfig;
import pl.chrono.timemanager.dto.response.WorkLogDto;
import pl.chrono.timemanager.exception.WorkException;

@SpringBootTest(classes = TestConfig.class)
@Transactional
class WorkLogServiceIT {

  @Autowired
  private WorkLogService workLogService;

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private WorkLogRepository workLogRepository;

  @Autowired
  private PolicyRepository policyRepository;

  @Autowired
  private UsersPolicyRepository usersPolicyRepository;

  @PersistenceContext
  private EntityManager entityManager;

  private User user;

  @BeforeEach
  void setUp() {
    user = userRepository.save(buildUser("Jan", "Kowalski", "jan.kowalski@test.com"));
    assignPolicy(user, 8, 10, 48, 0);
  }

  @Test
  void startWork_createsActiveWorkLog_whenNoneExists() {
    WorkLogDto result = workLogService.startWork(user.getUserId(), null);

    assertThat(result.getWorkLogId()).isNotNull();
    assertThat(result.getEndTime()).isNull();
    assertThat(workLogService.isUserWorking(user.getUserId())).isTrue();
  }

  @Test
  void startWork_throwsWorkStarted_whenAlreadyWorking() {
    workLogService.startWork(user.getUserId(), null);

    assertThatThrownBy(() -> workLogService.startWork(user.getUserId(), null)).isInstanceOf(
        WorkException.WorkStarted.class);
  }

  @Test
  void startWork_throwsAlreadyWorkedToday_whenClosedLogExistsToday() {
    LocalDate today = LocalDate.now();
    persistClosedWorkLog(user, today.atTime(2, 0), today.atTime(3, 0));

    assertThatThrownBy(() -> workLogService.startWork(user.getUserId(), null)).isInstanceOf(
        WorkException.AlreadyWorkedToday.class);
  }

  @Test
  void startWork_throwsNoHomeOfficeApplication_whenNoApprovedApplicationExists() {
    assertThatThrownBy(() -> workLogService.startWork(
        user.getUserId(),
        WorkLocationType.HOME_OFFICE
    )).isInstanceOf(WorkException.NoHomeOfficeApplication.class);
  }

  @Test
  void startWork_defaultsToOffice_whenNoLocationSpecifiedAndNoHomeOfficeApproval() {
    WorkLogDto result = workLogService.startWork(user.getUserId(), null);

    assertThat(result.getLocationType()).isEqualTo(WorkLocationType.OFFICE);
  }

  @Test
  void cancelWork_deletesActiveLog_whenWithinFifteenMinutes() {
    workLogService.startWork(user.getUserId(), null);

    workLogService.cancelWork(user.getUserId());

    assertThat(workLogService.isUserWorking(user.getUserId())).isFalse();
    assertThat(workLogRepository.findByUser_UserIdAndEndTimeIsNullAndPendingCorrectionFalse(user.getUserId())).isEmpty();
  }

  @Test
  void cancelWork_throwsCancellationExpired_whenStartedMoreThanFifteenMinutesAgo() {
    workLogService.startWork(user.getUserId(), null);
    backdateActiveLogStart(user, LocalDateTime.now().minusMinutes(20));

    assertThatThrownBy(() -> workLogService.cancelWork(user.getUserId())).isInstanceOf(
        WorkException.CancellationExpired.class);
    assertThat(workLogService.isUserWorking(user.getUserId())).isTrue();
  }

  @Test
  void cancelWork_throwsNoActiveWork_whenNothingToCancel() {
    assertThatThrownBy(() -> workLogService.cancelWork(user.getUserId())).isInstanceOf(
        WorkException.NoActiveWork.class);
  }

  @Test
  void endWork_closesLog_whenAccumulatedTimeWithinRequiredAndMaxRange() {
    workLogService.startWork(user.getUserId(), null);
    backdateActiveLogStart(user, LocalDateTime.now().minusHours(9));

    WorkLogDto result = workLogService.endWork(user.getUserId());

    assertThat(result.getEndTime()).isNotNull();
    assertThat(workLogService.isUserWorking(user.getUserId())).isFalse();
  }

  @Test
  void endWork_throwsInsufficientWorkTime_whenNotEnoughHoursAccumulated() {
    workLogService.startWork(user.getUserId(), null);

    assertThatThrownBy(() -> workLogService.endWork(user.getUserId())).isInstanceOf(
        WorkException.InsufficientWorkTime.class);
    assertThat(workLogService.isUserWorking(user.getUserId())).isTrue();
  }

  @Test
  void endWork_throwsNoActiveWork_whenNothingToClose() {
    assertThatThrownBy(() -> workLogService.endWork(user.getUserId())).isInstanceOf(
        WorkException.NoActiveWork.class);
  }

  @Test
  void hasUserWorkedToday_trueAfterClosedLogExistsForToday() {
    LocalDate today = LocalDate.now();
    persistClosedWorkLog(user, today.atTime(2, 0), today.atTime(11, 0));

    assertThat(workLogService.hasUserWorkedToday(user.getUserId())).isTrue();
  }

  @Test
  void hasUserWorkedToday_falseWhenNoLogsToday() {
    assertThat(workLogService.hasUserWorkedToday(user.getUserId())).isFalse();
  }

  @Test
  void getActiveWorkTime_returnsZero_whenNoActiveLog() {
    String activeTime = workLogService.getActiveWorkTime(user.getUserId());

    assertThat(activeTime).isEqualTo("00:00:00");
  }

  @Test
  void getActiveWorkTime_returnsNonZero_whenLogActive() {
    workLogService.startWork(user.getUserId(), null);
    backdateActiveLogStart(user, LocalDateTime.now().minusHours(2));

    String activeTime = workLogService.getActiveWorkTime(user.getUserId());

    assertThat(activeTime).isNotEqualTo("00:00:00");
  }

  private User buildUser(String name, String surname, String email) {
    User user = new User();
    user.setName(name);
    user.setSurname(surname);
    user.setEmail(email);
    user.setPasswordHash("hash");
    return user;
  }

  private void assignPolicy(User user, int dailyHours, int maxDailyHours, int maxWeeklyHours,
                            int roundingMinutes) {
    Policy policy = new Policy();
    policy.setName("Standard 8h - all days");
    policy.setDailyHours(dailyHours);
    policy.setMaxDailyHours(maxDailyHours);
    policy.setMaxWeeklyHours(maxWeeklyHours);
    policy.setRoundingMinutes(roundingMinutes);
    policy.setWorkingDays(new HashSet<>(Arrays.asList(DayOfWeek.values())));
    policyRepository.save(policy);

    UsersPolicy usersPolicy = new UsersPolicy();
    usersPolicy.setUser(user);
    usersPolicy.setPolicy(policy);
    usersPolicy.setValidFrom(LocalDate.now().minusYears(1));
    usersPolicy.setValidTo(null);
    usersPolicyRepository.save(usersPolicy);

    entityManager.flush();
    entityManager.clear();
  }

  private void persistClosedWorkLog(User user, LocalDateTime startTime, LocalDateTime endTime) {
    WorkLog workLog = new WorkLog();
    workLog.setUser(user);
    workLog.setStartTime(startTime);
    workLog.setEndTime(endTime);
    workLog.setLocationType(WorkLocationType.OFFICE);
    workLogRepository.save(workLog);
  }

  private void backdateActiveLogStart(User user, LocalDateTime newStartTime) {
    WorkLog active = workLogRepository.findByUser_UserIdAndEndTimeIsNullAndPendingCorrectionFalse(user.getUserId())
        .orElseThrow();
    active.setStartTime(newStartTime);
    workLogRepository.save(active);
  }
}
