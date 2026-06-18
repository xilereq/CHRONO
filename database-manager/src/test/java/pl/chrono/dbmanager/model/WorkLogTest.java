package pl.chrono.dbmanager.model;

import java.time.LocalDateTime;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class WorkLogTest {

  private final LocalDateTime boundaryTime = LocalDateTime.of(2026, 6, 9, 16, 0);
  private WorkLog workLog;

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
  void roundingAppliedTest() {
    workLog.setStartTime(LocalDateTime.of(2026, 6, 9, 8, 0));
    workLog.setEndTime(LocalDateTime.of(2026, 6, 9, 15, 50));

    int requiredSeconds = 8 * 3600;
    int roundingSeconds = 15 * 60;

    long effectiveTime = workLog.getEffectiveWorkTime(boundaryTime, requiredSeconds, roundingSeconds);

    assertThat(effectiveTime).isEqualTo(requiredSeconds);
  }

  @Test
  void roundingIgnoreWithInsufficientTimeTest() {
    workLog.setStartTime(LocalDateTime.of(2026, 6, 9, 8, 0));
    workLog.setEndTime(LocalDateTime.of(2026, 6, 9, 15, 30));

    int requiredSeconds = 8 * 3600;
    int roundingSeconds = 15 * 60;

    long effectiveTime = workLog.getEffectiveWorkTime(boundaryTime, requiredSeconds, roundingSeconds);

    assertThat(effectiveTime).isEqualTo(7 * 3600 + 30 * 60);
  }


  @Test
  void roundingIgnoreOnExceedingTimeTest() {
    workLog.setStartTime(LocalDateTime.of(2026, 6, 9, 8, 59));
    workLog.setEndTime(LocalDateTime.of(2026, 6, 9, 17, 0));

    int requiredSeconds = 8 * 3600;
    int roundingSeconds = 15 * 60;

    long effectiveTime = workLog.getEffectiveWorkTime(boundaryTime, requiredSeconds, roundingSeconds);

    assertThat(effectiveTime).isEqualTo(8 * 3600 + 60);
  }

}
