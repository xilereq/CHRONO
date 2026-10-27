package pl.chrono.reportmanager.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.LocalTime;
import org.junit.jupiter.api.Test;

class ValueTypeTest {

  @Test
  void shouldFormatDurationAsHoursAndMinutes() {
    assertThat(ValueType.DURATION.format(0L)).isEqualTo("0:00");
    assertThat(ValueType.DURATION.format(3660L)).isEqualTo("1:01");
    assertThat(ValueType.DURATION.format(144000L)).isEqualTo("40:00");
    assertThat(ValueType.DURATION.format(-5400L)).isEqualTo("-1:30");
    assertThat(ValueType.DURATION.format(28800)).isEqualTo("8:00");
  }

  @Test
  void shouldFormatTimeAndShowDashWhenMissing() {
    assertThat(ValueType.TIME.format(LocalTime.of(8, 5))).isEqualTo("08:05");
    assertThat(ValueType.TIME.format(null)).isEqualTo("-");
  }

  @Test
  void shouldFormatOtherValuesAsText() {
    assertThat(ValueType.DATE.format(LocalDate.of(2026, 10, 1))).isEqualTo("2026-10-01");
    assertThat(ValueType.NUMBER.format(7)).isEqualTo("7");
    assertThat(ValueType.TEXT.format("Jan")).isEqualTo("Jan");
    assertThat(ValueType.TEXT.format(null)).isEmpty();
  }
}
