package pl.chrono.dbmanager.model;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class WorkLogTest {

  private final LocalDateTime boundaryTime = LocalDateTime.of(2026, 6, 9, 16, 0);
  private WorkLog workLog;

  private Policy createTestPolicy() {
    Policy policy = new Policy();
    policy.setWorkingDays(Set.of(
        DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY,
        DayOfWeek.FRIDAY
    ));
    policy.setDailyHours(8);
    policy.setRoundingMinutes(15);
    return policy;
  }

  @BeforeEach
  void setUp() {
    workLog = new WorkLog();
  }


  @Test
  void nullStartTimeTest() {
    workLog.setStartTime(null);
    assertThat(workLog.calculateWorkTime(boundaryTime)).isZero();
  }

  @Test
  void endTimeBeforeStartTimeTest() {
    workLog.setStartTime(LocalDateTime.of(2026, 6, 9, 17, 0));
    workLog.setEndTime(LocalDateTime.of(2026, 6, 9, 15, 0));

    assertThat(workLog.calculateWorkTime(boundaryTime)).isZero();
  }

  @Test
  void noBreaksCalculationTest() {
    workLog.setStartTime(LocalDateTime.of(2026, 6, 9, 8, 0));
    workLog.setEndTime(LocalDateTime.of(2026, 6, 9, 16, 0));

    assertThat(workLog.calculateWorkTime(boundaryTime)).isEqualTo(8 * 3600);
  }

  @Test
  void calculationTimeWithBreakTest() {
    workLog.setStartTime(LocalDateTime.of(2026, 6, 9, 8, 0));
    workLog.setEndTime(LocalDateTime.of(2026, 6, 9, 16, 0));

    LeaveLog breakLog = new LeaveLog();
    breakLog.setStartTime(LocalDateTime.of(2026, 6, 9, 12, 0));
    breakLog.setEndTime(LocalDateTime.of(2026, 6, 9, 12, 30));
    workLog.setLeaveLogs(List.of(breakLog));

    assertThat(workLog.calculateWorkTime(boundaryTime)).isEqualTo(27000);
  }

  @Test
  void breakTimeCalculationTest() {
    LeaveLog breakLog = new LeaveLog();
    breakLog.setStartTime(LocalDateTime.of(2026, 6, 9, 15, 30));
    breakLog.setEndTime(null);
    workLog.setLeaveLogs(List.of(breakLog));

    assertThat(workLog.calculateBreakTime(boundaryTime)).isEqualTo(1800);
  }

  @Test
  void breakTimeEndedBeforeStartCalculationTest() {
    LeaveLog breakLog = new LeaveLog();
    breakLog.setStartTime(LocalDateTime.of(2026, 6, 9, 15, 30));
    breakLog.setEndTime(LocalDateTime.of(2026, 6, 9, 15, 0));
    workLog.setLeaveLogs(List.of(breakLog));

    assertThat(workLog.calculateBreakTime(boundaryTime)).isEqualTo(0);
  }

  @Test()
  void breakTimeEmptyListCalculationTest() {
    workLog.setLeaveLogs(List.of());

    assertThat(workLog.calculateBreakTime(boundaryTime)).isEqualTo(0);
  }

  @Test()
  void breakTimeNullListCalculationTest() {
    workLog.setLeaveLogs(null);

    assertThat(workLog.calculateBreakTime(boundaryTime)).isEqualTo(0);
  }

  @Test
  void noPolicyDefaultTest() {
    assertThat(workLog.getRequiredSeconds()).isEqualTo(8 * 3600);
    assertThat(workLog.getMaxAllowedSeconds()).isEqualTo(12 * 3600);
    assertThat(workLog.getRoundingSeconds()).isZero();
  }

  @Test
  void maxAllowedFromPolicyTest() {
    User testUser = new User();
    Policy policy = new Policy();
    policy.setMaxDailyHours(10);

    UsersPolicy up = new UsersPolicy();
    up.setValidFrom(LocalDate.now().minusDays(1));
    up.setPolicy(policy);
    testUser.setUserPolicies(List.of(up));
    workLog.setUser(testUser);

    int maxAllowed = workLog.getMaxAllowedSeconds();

    assertThat(maxAllowed).isEqualTo(10 * 3600);
  }

  @Test
  void nullStartTimeForceCloseTest() {
    workLog.setStartTime(null);

    workLog.forceClose(boundaryTime);

    assertThat(workLog.getEndTime()).isNull();
  }

  @Test
  void nullStartTimeRequiredSecondsTest() {
    workLog.setStartTime(null);

    int requiredSeconds = workLog.getRequiredSeconds();

    assertThat(requiredSeconds).isEqualTo(8 * 3600);
  }

  @Test
  void notNullUserButNullPolicyTest() {
    User testUser = new User();
    testUser.setUserPolicies(List.of());
    workLog.setUser(testUser);
    workLog.setStartTime(LocalDateTime.of(2026, 6, 9, 8, 0));

    assertThat(workLog.getRequiredSeconds()).isEqualTo(8 * 3600);
    assertThat(workLog.getMaxAllowedSeconds()).isEqualTo(12 * 3600);
    assertThat(workLog.getRoundingSeconds()).isZero();
  }

  @Test
  void everythingSetButNullStartTimeTest() {
    User testUser = new User();
    Policy policy = createTestPolicy();
    UsersPolicy up = new UsersPolicy();
    up.setValidFrom(LocalDate.now().minusDays(1));
    up.setPolicy(policy);
    testUser.setUserPolicies(List.of(up));
    workLog.setUser(testUser);
    workLog.setStartTime(null);

    assertThat(workLog.getRequiredSeconds()).isEqualTo(8 * 3600);
  }

  @Test
  void nullRoundingMinutesTest() {
    User testUser = new User();
    Policy policy = createTestPolicy();
    policy.setRoundingMinutes(null);

    UsersPolicy up = new UsersPolicy();
    up.setValidFrom(LocalDate.now().minusDays(1));
    up.setPolicy(policy);
    testUser.setUserPolicies(List.of(up));
    workLog.setUser(testUser);

    int roundingSeconds = workLog.getRoundingSeconds();

    assertThat(roundingSeconds).isZero();
  }

  @Test
  void endTimeWithRoundingTest() {
    User testUser = new User();
    Policy policy = createTestPolicy();

    UsersPolicy up = new UsersPolicy();
    up.setPolicy(policy);
    up.setValidFrom(LocalDate.now().minusDays(1));
    testUser.setUserPolicies(List.of(up));

    workLog.setUser(testUser);

    workLog.setStartTime(LocalDateTime.of(2026, 6, 9, 8, 0));
    workLog.setEndTime(LocalDateTime.of(2026, 6, 9, 15, 50));

    long effectiveTime = workLog.getEffectiveWorkTime(boundaryTime);

    assertThat(effectiveTime).isEqualTo(8 * 3600);
  }

  @Test
  void endTimeWithoutRoundingTest() {
    User testUser = new User();
    Policy policy = createTestPolicy();

    UsersPolicy up = new UsersPolicy();
    up.setValidFrom(LocalDate.now().minusDays(1));
    up.setPolicy(policy);
    testUser.setUserPolicies(List.of(up));
    workLog.setUser(testUser);

    workLog.setStartTime(LocalDateTime.of(2026, 6, 9, 8, 0));
    workLog.setEndTime(LocalDateTime.of(2026, 6, 9, 15, 30));

    long effectiveTime = workLog.getEffectiveWorkTime(boundaryTime);

    long expectedExactTime = 7 * 3600 + 30 * 60;

    assertThat(effectiveTime).isEqualTo(expectedExactTime);
  }


  @Test
  void forceCloseTest() {
    workLog.setStartTime(LocalDateTime.of(2026, 6, 9, 8, 0));

    LeaveLog openBreak = new LeaveLog();
    openBreak.setStartTime(LocalDateTime.of(2026, 6, 9, 12, 0));
    workLog.setLeaveLogs(List.of(openBreak));

    workLog.forceClose(boundaryTime);

    LocalDateTime expectedCloseTime = LocalDateTime.of(2026, 6, 9, 20, 0);

    assertThat(workLog.getEndTime()).isEqualTo(expectedCloseTime);
    assertThat(openBreak.getEndTime()).isEqualTo(expectedCloseTime);
  }

  @Test
  void nullStartTimeForForceCloseTest() {
    workLog.setStartTime(null);

    workLog.forceClose(boundaryTime);

    assertThat(workLog.getEndTime()).isNull();
  }

  @Test
  void emptyListForForceCloseTest() {
    workLog.setStartTime(LocalDateTime.of(2026, 6, 9, 8, 0));
    workLog.setLeaveLogs(null);

    workLog.forceClose(boundaryTime);

    assertThat(workLog.getEndTime()).isNotNull();
  }

  @Test
  void forceCloseWithClosedBreakLogsTest() {
    workLog.setStartTime(LocalDateTime.of(2026, 6, 9, 8, 0));

    LeaveLog closedLeave = new LeaveLog();
    closedLeave.setStartTime(LocalDateTime.of(2026, 6, 9, 12, 0));
    closedLeave.setEndTime(LocalDateTime.of(2026, 6, 9, 12, 30));

    LeaveLog openLeave = new LeaveLog();
    openLeave.setStartTime(LocalDateTime.of(2026, 6, 9, 14, 0));
    openLeave.setEndTime(null);

    workLog.setLeaveLogs(List.of(closedLeave, openLeave));

    workLog.forceClose(boundaryTime);

    assertThat(closedLeave.getEndTime()).isEqualTo(LocalDateTime.of(2026, 6, 9, 12, 30));
    assertThat(openLeave.getEndTime()).isNotNull();
  }

  @Test
  void roundingIsIgnoredForExceedingTimeTest() {
    User testUser = new User();
    Policy policy = new Policy();
    policy.setDailyHours(8);
    policy.setRoundingMinutes(15);
    policy.setWorkingDays(Set.of(
        DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY,
        DayOfWeek.FRIDAY
    ));

    UsersPolicy up = new UsersPolicy();
    up.setValidFrom(LocalDate.now().minusDays(1));
    up.setPolicy(policy);
    testUser.setUserPolicies(List.of(up));
    workLog.setUser(testUser);

    workLog.setStartTime(LocalDateTime.of(2026, 6, 9, 8, 0));
    workLog.setEndTime(LocalDateTime.of(2026, 6, 9, 17, 0));

    long effectiveTime = workLog.getEffectiveWorkTime(boundaryTime);

    assertThat(effectiveTime).isEqualTo(9 * 3600);
  }
}
