package pl.chrono.reportmanager.generator;

import org.springframework.stereotype.Component;
import pl.chrono.reportmanager.dto.EmployeeOvertimeReport;
import pl.chrono.reportmanager.dto.ReportFormat;
import pl.chrono.reportmanager.exception.ReportGenerationException;

import java.nio.charset.StandardCharsets;

@Component
public class EmployeeOvertimeReportGenerator implements I_ReportGenerator<EmployeeOvertimeReport> {

  @Override
  public byte[] generate(EmployeeOvertimeReport report, ReportFormat format) {
    return switch (format) {
      case CSV -> generateCsv(report);
      case PDF -> throw new ReportGenerationException.UnsupportedFormat(format.name());
      case XLSX -> throw new ReportGenerationException.UnsupportedFormat(format.name());
    };
  }

  private byte[] generateCsv(EmployeeOvertimeReport report) {
    StringBuilder sb = new StringBuilder();
    sb.append("Raport Nadgodzin (Od - Do):;").append(report.startDate()).append(" - ").append(report.endDate()).append("\n");
    sb.append("Pracownik:;").append(report.employeeFullName()).append("\n");
    sb.append("Suma nadgodzin:;").append(formatSeconds(report.totalOvertimeSeconds())).append("\n\n");

    sb.append("Data;Przepracowano;Wymagano;Nadgodziny\n");

    report.entries().forEach(e -> {
      sb.append(e.date()).append(";")
          .append(formatSeconds(e.workedSeconds())).append(";")
          .append(formatSeconds((long) e.requiredSeconds())).append(";")
          .append(formatSeconds(e.overtimeSeconds())).append("\n");
    });

    return "\uFEFF".getBytes(StandardCharsets.UTF_8);
  }
}
