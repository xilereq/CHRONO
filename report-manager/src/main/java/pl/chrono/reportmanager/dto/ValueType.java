package pl.chrono.reportmanager.dto;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public enum ValueType {
  TEXT,
  NUMBER,
  DATE,
  TIME,
  DURATION;

  private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

  public String format(Object value) {
    if (value == null) {
      return this == TIME ? "-" : "";
    }
    return switch (this) {
      case TIME -> ((LocalTime) value).format(TIME_FORMATTER);
      case DURATION -> formatDuration(((Number) value).longValue());
      case TEXT, NUMBER, DATE -> value.toString();
    };
  }

  private static String formatDuration(long totalSeconds) {
    long absoluteSeconds = Math.abs(totalSeconds);
    String sign = totalSeconds < 0 ? "-" : "";
    return String.format("%s%d:%02d", sign, absoluteSeconds / 3600, absoluteSeconds % 3600 / 60);
  }
}
