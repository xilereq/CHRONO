package pl.chrono.reportmanager.generator;

import pl.chrono.reportmanager.dto.ReportFormat;
import java.nio.charset.StandardCharsets;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public interface I_ReportGenerator<T> {
  DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

  byte[] generate(T reportData, ReportFormat format);

  default String formatSeconds(Long totalSeconds) {
    if (totalSeconds == null) return "0:00";
    long absSeconds = Math.abs(totalSeconds);
    long hours = absSeconds / 3600;
    long minutes = absSeconds % 3600 / 60;
    String sign = totalSeconds < 0 ? "-" : "";
    return String.format("%s%d:%02d", sign, hours, minutes);
  }

  default byte[] addUtf8Bom(String content) {
    return ("\uFEFF" + content).getBytes(StandardCharsets.UTF_8);
  }

  default String formatTime(LocalTime time) {
    return time != null ? time.format(TIME_FORMATTER) : "-";
  }
}
