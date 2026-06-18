package pl.chrono.reportmanager.generator;

import org.junit.jupiter.api.Test;

import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;

class I_ReportGeneratorTest {

  private final I_ReportGenerator<Object> generator = (reportData, format) -> new byte[0];

  @Test
  void shouldFormatSecondsCorrectly() {
    assertThat(generator.formatSeconds(null)).isEqualTo("0:00");
    assertThat(generator.formatSeconds(3600L)).isEqualTo("1:00");
    assertThat(generator.formatSeconds(5400L)).isEqualTo("1:30");
    assertThat(generator.formatSeconds(3660L)).isEqualTo("1:01");
    assertThat(generator.formatSeconds(0L)).isEqualTo("0:00");
    assertThat(generator.formatSeconds(-3600L)).isEqualTo("-1:00");
    assertThat(generator.formatSeconds(-5400L)).isEqualTo("-1:30");
  }

  @Test
  void shouldAddUtf8Bom() {
    byte[] result = generator.addUtf8Bom("test");
    assertThat(result).hasSize(7);
    assertThat(result[0]).isEqualTo((byte) 0xEF);
    assertThat(result[1]).isEqualTo((byte) 0xBB);
    assertThat(result[2]).isEqualTo((byte) 0xBF);
  }

  @Test
  void shouldFormatTimeCorrectly() {
    assertThat(generator.formatTime(null)).isEqualTo("-");
    assertThat(generator.formatTime(LocalTime.of(8, 5))).isEqualTo("08:05");
    assertThat(generator.formatTime(LocalTime.of(15, 30))).isEqualTo("15:30");
  }
}
