package pl.chrono.timemanager.service;

import java.time.LocalDate;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import pl.chrono.dbmanager.model.Application;
import pl.chrono.dbmanager.model.Policy;
import pl.chrono.dbmanager.model.enums.ApplicationType;
import pl.chrono.dbmanager.repository.ApplicationRepository;

@ExtendWith(MockitoExtension.class)
class WorkTimeCalculationServiceTest {

  @Mock
  private HolidayService holidayService;

  @Mock
  private ApplicationRepository applicationRepository;

  @InjectMocks
  private WorkTimeCalculationService service;

  @Test
  void calculateWorkingDaysNullDatesTest() {
    assertThat(service.calculateWorkingDays(null, LocalDate.now(), null)).isZero();
    assertThat(service.calculateWorkingDays(LocalDate.now(), null, null)).isZero();
  }

  @Test
  void calculateWorkingDaysStartDateAfterEndDateTest() {
    LocalDate start = LocalDate.now().plusDays(1);
    LocalDate end = LocalDate.now();
    assertThat(service.calculateWorkingDays(start, end, null)).isZero();
  }

  @Test
  void calculateWorkingDaysValidRangeTest() {
    LocalDate start = LocalDate.of(2026, 6, 1);
    LocalDate end = LocalDate.of(2026, 6, 3);
    Policy policy = mock(Policy.class);

    when(policy.getRequiredSecondsForDate(start)).thenReturn(8 * 3600);
    when(policy.getRequiredSecondsForDate(start.plusDays(1))).thenReturn(0);
    when(policy.getRequiredSecondsForDate(start.plusDays(2))).thenReturn(4 * 3600);

    int days = service.calculateWorkingDays(start, end, policy);

    assertThat(days).isEqualTo(2);
  }

  @Test
  void getBaseRequiredSecondsNotNullPolicyTest() {
    LocalDate date = LocalDate.of(2026, 6, 1);
    Policy policy = mock(Policy.class);
    when(policy.getRequiredSecondsForDate(date)).thenReturn(3600);

    assertThat(service.getBaseRequiredSeconds(date, policy)).isEqualTo(3600);
  }

  @Test
  void getBaseRequiredSecondsNullPolicyForSaturdayTest() {
    LocalDate saturday = LocalDate.of(2026, 6, 6);
    assertThat(service.getBaseRequiredSeconds(saturday, null)).isZero();
  }

  @Test
  void getBaseRequiredSecondsNullPolicyForSundayTest() {
    LocalDate sunday = LocalDate.of(2026, 6, 7);
    assertThat(service.getBaseRequiredSeconds(sunday, null)).isZero();
  }

  @Test
  void getBaseRequiredSecondsHolidayTest() {
    LocalDate date = LocalDate.of(2026, 6, 4);
    when(holidayService.isHoliday(date)).thenReturn(true);

    assertThat(service.getBaseRequiredSeconds(date, null)).isZero();
  }

  @Test
  void getBaseRequiredSecondsNormalWeekdayTest() {
    LocalDate thursday = LocalDate.of(2026, 6, 4);
    when(holidayService.isHoliday(thursday)).thenReturn(false);

    assertThat(service.getBaseRequiredSeconds(thursday, null)).isEqualTo(8 * 3600);
  }

  @Test
  void getAdjustedRequiredSecondsNoApplicationsTest() {
    LocalDate date = LocalDate.of(2026, 6, 4);
    Policy policy = mock(Policy.class);
    when(policy.getRequiredSecondsForDate(date)).thenReturn(8 * 3600);

    when(applicationRepository.findApprovedByEmployeeAndDate(1, date)).thenReturn(List.of());

    assertThat(service.getAdjustedRequiredSeconds(1, date, policy)).isEqualTo(8 * 3600);
  }

  @Test
  void getAdjustedRequiredSecondsApplicationsReduceTimeTest() {
    LocalDate date = LocalDate.of(2026, 6, 4);
    Policy policy = mock(Policy.class);
    when(policy.getRequiredSecondsForDate(date)).thenReturn(8 * 3600);

    Application appEarlyExit = new Application();
    appEarlyExit.setType(ApplicationType.EARLY_EXIT);
    appEarlyExit.setDurationSeconds(3600);

    Application appOvertimeTake = new Application();
    appOvertimeTake.setType(ApplicationType.OVERTIME_TAKE);
    appOvertimeTake.setDurationSeconds(1800);

    Application appOvertimeEarn = new Application();
    appOvertimeEarn.setType(ApplicationType.OVERTIME_EARN);
    appOvertimeEarn.setDurationSeconds(7200);

    Application appHoliday = new Application();
    appHoliday.setType(ApplicationType.HOLIDAY_LEAVE);
    appHoliday.setDurationSeconds(1000);

    when(applicationRepository.findApprovedByEmployeeAndDate(1, date)).thenReturn(
        List.of(appEarlyExit, appOvertimeTake, appOvertimeEarn, appHoliday));

    assertThat(service.getAdjustedRequiredSeconds(1, date, policy)).isEqualTo(30600);
  }

  @Test
  void getAdjustedRequiredSecondsNullDurationTest() {
    LocalDate date = LocalDate.of(2026, 6, 4);
    Policy policy = mock(Policy.class);
    when(policy.getRequiredSecondsForDate(date)).thenReturn(8 * 3600);

    Application appEarlyExit = new Application();
    appEarlyExit.setType(ApplicationType.EARLY_EXIT);
    appEarlyExit.setDurationSeconds(null);

    when(applicationRepository.findApprovedByEmployeeAndDate(1, date)).thenReturn(
        List.of(appEarlyExit));

    assertThat(service.getAdjustedRequiredSeconds(1, date, policy)).isEqualTo(8 * 3600);
  }

  @Test
  void getAdjustedRequiredSecondsNegativeResultTest() {
    LocalDate date = LocalDate.of(2026, 6, 4);
    Policy policy = mock(Policy.class);
    when(policy.getRequiredSecondsForDate(date)).thenReturn(2 * 3600);

    Application appEarlyExit = new Application();
    appEarlyExit.setType(ApplicationType.EARLY_EXIT);
    appEarlyExit.setDurationSeconds(4 * 3600);

    when(applicationRepository.findApprovedByEmployeeAndDate(1, date)).thenReturn(
        List.of(appEarlyExit));

    assertThat(service.getAdjustedRequiredSeconds(1, date, policy)).isZero();
  }

  @Test
  void hasApprovedHomeOfficeWithHOApplicationTest() {
    LocalDate date = LocalDate.now();
    Application app = new Application();
    app.setType(ApplicationType.HOME_OFFICE);

    when(applicationRepository.findApprovedByEmployeeAndDate(1, date)).thenReturn(List.of(app));

    assertThat(service.hasApprovedHomeOffice(1, date)).isTrue();
  }

  @Test
  void hasApprovedHomeOfficeWithoutHOApplicationTest() {
    LocalDate date = LocalDate.now();
    Application app = new Application();
    app.setType(ApplicationType.HOLIDAY_LEAVE);

    when(applicationRepository.findApprovedByEmployeeAndDate(1, date)).thenReturn(List.of(app));

    assertThat(service.hasApprovedHomeOffice(1, date)).isFalse();
  }

  @Test
  void hasApprovedHomeOfficeEmptyListTest() {
    LocalDate date = LocalDate.now();
    when(applicationRepository.findApprovedByEmployeeAndDate(1, date)).thenReturn(List.of());

    assertThat(service.hasApprovedHomeOffice(1, date)).isFalse();
  }

  @Test
  void formatSecondsToTimeConvertTest() {
    assertThat(service.formatSecondsToTime(0)).isEqualTo("00:00:00");
    assertThat(service.formatSecondsToTime(3600)).isEqualTo("01:00:00");
    assertThat(service.formatSecondsToTime(3665)).isEqualTo("01:01:05");
    assertThat(service.formatSecondsToTime(3600 * 25 + 60 * 5 + 1)).isEqualTo("25:05:01");
  }

  @Test
  void getMaxAllowedSecondsNoPolicyTest() {
    assertThat(service.getMaxAllowedSeconds(null)).isEqualTo(12 * 3600);
  }

  @Test
  void getMaxAllowedSecondsNullValueTest() {
    Policy policy = new Policy();
    policy.setMaxDailyHours(null);

    assertThat(service.getMaxAllowedSeconds(policy)).isEqualTo(12 * 3600);
  }

  @Test
  void getMaxAllowedSecondsPolicySetTest() {
    Policy policy = new Policy();
    policy.setMaxDailyHours(10);

    assertThat(service.getMaxAllowedSeconds(policy)).isEqualTo(10 * 3600);
  }

  @Test
  void getRoundingSecondsNullPolicyTest() {
    assertThat(service.getRoundingSeconds(null)).isZero();
  }

  @Test
  void getRoundingSecondsNullValueTest() {
    Policy policy = new Policy();
    policy.setRoundingMinutes(null);

    assertThat(service.getRoundingSeconds(policy)).isZero();
  }

  @Test
  void getRoundingSecondsPolicySetTest() {
    Policy policy = new Policy();
    policy.setRoundingMinutes(15);

    assertThat(service.getRoundingSeconds(policy)).isEqualTo(900);
  }
}
