package pl.chrono.reportmanager.dto;

import java.time.LocalDate;

public record ReportField(String label, Object value, ValueType type) {

  public static ReportField period(LocalDate startDate, LocalDate endDate) {
    return new ReportField("Okres", startDate + " - " + endDate, ValueType.TEXT);
  }

  public String formattedValue() {
    return type.format(value);
  }
}
