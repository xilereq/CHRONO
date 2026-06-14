package pl.chrono.reportmanager.generator;

import org.springframework.stereotype.Component;
import pl.chrono.reportmanager.dto.ReportFormat;
import pl.chrono.reportmanager.dto.TeamOvertimeReport;
import pl.chrono.reportmanager.exception.ReportGenerationException;

import java.nio.charset.StandardCharsets;

@Component
public class TeamOvertimeReportGenerator implements I_ReportGenerator<TeamOvertimeReport> {

  @Override
  public byte[] generate(TeamOvertimeReport report, ReportFormat format) {
    return switch (format) {
      case CSV -> generateCsv(report);
      case PDF -> throw new ReportGenerationException.UnsupportedFormat(format.name());
      case XLSX -> throw new ReportGenerationException.UnsupportedFormat(format.name());
    };
  }

  private byte[] generateCsv(TeamOvertimeReport report) {
    StringBuilder sb = new StringBuilder();
    sb.append("Zbiorczy Raport Nadgodzin Zespolu (Od - Do):;").append(report.startDate()).append(" - ").append(report.endDate()).append("\n\n");
    sb.append("ID Pracownika;Imie i Nazwisko;Suma Nadgodzin\n");

    report.entries().forEach(e -> {
      sb.append(e.employeeId()).append(";")
          .append(e.employeeFullName()).append(";")
          .append(formatSeconds(e.totalOvertimeSeconds())).append("\n");
    });

    return "\uFEFF".getBytes(StandardCharsets.UTF_8);
  }
}
