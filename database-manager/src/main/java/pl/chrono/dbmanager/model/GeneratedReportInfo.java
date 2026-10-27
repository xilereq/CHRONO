package pl.chrono.dbmanager.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import pl.chrono.dbmanager.model.enums.ReportType;

public interface GeneratedReportInfo {
  Long getId();

  ReportType getReportType();

  String getFormat();

  LocalDate getPeriodStart();

  LocalDate getPeriodEnd();

  String getFileName();

  long getFileSize();

  LocalDateTime getCreatedAt();
}
