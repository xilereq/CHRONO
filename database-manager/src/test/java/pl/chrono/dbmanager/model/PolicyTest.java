package pl.chrono.dbmanager.model;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PolicyTest {

  private Policy policy;

  @BeforeEach
  void setUp() {
    policy = new Policy();
    policy.setWorkingDays(Set.of(
        DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY,
        DayOfWeek.FRIDAY
    ));
  }


  @Test
  void maxAllowedSecondsTest() {
    policy.setMaxDailyHours(10);

    int result = policy.getMaxAllowedSeconds();

    assertThat(result).isEqualTo(10 * 3600);
  }

  @Test
  void maxAllowedSecondsDefaultTest() {
    policy.setMaxDailyHours(null);

    int result = policy.getMaxAllowedSeconds();

    assertThat(result).isEqualTo(12 * 3600);
  }


  @Test
  void nullDateTest() {
    int result = policy.getRequiredSecondsForDate(null);

    assertThat(result).isZero();
  }

  @Test
  void notWorkingDayTest() {
    LocalDate saturday = LocalDate.of(2026, 6, 13);

    int result = policy.getRequiredSecondsForDate(saturday);

    assertThat(result).isZero();
  }

  @Test
  void workingDayTest() {
    policy.setDailyHours(7);
    LocalDate monday = LocalDate.of(2026, 6, 8);

    int result = policy.getRequiredSecondsForDate(monday);

    assertThat(result).isEqualTo(7 * 3600);
  }

  @Test
  void defaultDailyHoursTest() {
    policy.setDailyHours(null);
    LocalDate monday = LocalDate.of(2026, 6, 8);

    int result = policy.getRequiredSecondsForDate(monday);

    assertThat(result).isEqualTo(8 * 3600);
  }

  @Test
  void holidayDailyHoursTest() {
    policy.setDailyHours(8);
    LocalDate holidayDate = LocalDate.of(2026, 6, 4);

    Holiday holiday = new Holiday();
    holiday.setDate(holidayDate);

    HolidayPolicy holidayPolicy = new HolidayPolicy();
    holidayPolicy.setHoliday(holiday);

    policy.setHolidayPolicies(List.of(holidayPolicy));

    int result = policy.getRequiredSecondsForDate(holidayDate);

    assertThat(result).isZero();
  }

  @Test
  void noHolidayPoliciesTest() {
    policy.setDailyHours(4);
    policy.setHolidayPolicies(null);
    LocalDate monday = LocalDate.of(2026, 6, 8);

    int result = policy.getRequiredSecondsForDate(monday);

    assertThat(result).isEqualTo(4 * 3600);
  }

  @Test
  void holidayWithNullDateTest() {
    policy.setDailyHours(4);

    Holiday nullDateHoliday = new Holiday();
    nullDateHoliday.setDate(null);
    HolidayPolicy nullDateHolidayPolicy = new HolidayPolicy();
    nullDateHolidayPolicy.setHoliday(nullDateHoliday);

    HolidayPolicy nullHolidayPolicy = new HolidayPolicy();
    nullHolidayPolicy.setHoliday(null);

    policy.setHolidayPolicies(List.of(nullDateHolidayPolicy, nullHolidayPolicy));

    LocalDate monday = LocalDate.of(2026, 6, 8);

    int result = policy.getRequiredSecondsForDate(monday);

    assertThat(result).isEqualTo(4 * 3600);
  }

  @Test
  void differentMonthTest() {
    policy.setDailyHours(3);

    Holiday holiday = new Holiday();
    holiday.setDate(LocalDate.of(2026, 6, 15));

    HolidayPolicy holidayPolicy = new HolidayPolicy();
    holidayPolicy.setHoliday(holiday);
    policy.setHolidayPolicies(List.of(holidayPolicy));

    LocalDate dateToCheck = LocalDate.of(2026, 6, 15);
    LocalDate nextMonth = LocalDate.of(2026, 7, 15);

    int result = policy.getRequiredSecondsForDate(dateToCheck);
    int resultNextMonth = policy.getRequiredSecondsForDate(nextMonth);

    assertThat(result).isZero();
    assertThat(resultNextMonth).isEqualTo(3 * 3600);
  }

  @Test
  void differentDayTest() {
    policy.setDailyHours(3);

    Holiday holiday = new Holiday();
    holiday.setDate(LocalDate.of(2026, 6, 15));

    HolidayPolicy holidayPolicy = new HolidayPolicy();
    holidayPolicy.setHoliday(holiday);
    policy.setHolidayPolicies(List.of(holidayPolicy));

    LocalDate dateToCheck = LocalDate.of(2026, 6, 15);
    LocalDate nextDay = LocalDate.of(2026, 6, 16);

    int result = policy.getRequiredSecondsForDate(dateToCheck);
    int resultNextDay = policy.getRequiredSecondsForDate(nextDay);

    assertThat(result).isZero();
    assertThat(resultNextDay).isEqualTo(3 * 3600);
  }
}
