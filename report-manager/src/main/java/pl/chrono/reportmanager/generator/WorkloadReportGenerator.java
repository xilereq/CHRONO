package pl.chrono.reportmanager.generator;

import org.springframework.stereotype.Component;
import pl.chrono.reportmanager.dto.ReportFormat;
import pl.chrono.reportmanager.dto.TeamWorkloadReport;
import java.nio.charset.StandardCharsets;

@Component
public class WorkloadReportGenerator implements I_ReportGenerator<TeamWorkloadReport> {

  @Override
  public byte[] generate(TeamWorkloadReport report, ReportFormat format) {
    return switch (format) {
      case CSV -> generateCsv(report);
      case PDF -> throw new UnsupportedOperationException("Eksport do PDF wymaga dodania biblioteki");
      case XLSX -> throw new UnsupportedOperationException("Eksport do XLSX wymaga dodania biblioteki");
    };
  }

  private byte[] generateCsv(TeamWorkloadReport report) {
    StringBuilder sb = new StringBuilder();
    sb.append("Raport Obciazenia Zespolu (Od - Do):;").append(report.startDate()).append(" - ").append(report.endDate()).append("\n\n");
    sb.append("ID Pracownika;Imie i Nazwisko;Przepracowano;Wymagano;Bilans (Nadgodziny)\n");

    report.entries().forEach(e -> {
      sb.append(e.employeeId()).append(";")
          .append(e.employeeFullName()).append(";")
          .append(formatSeconds(e.totalWorkSeconds())).append(";")
          .append(formatSeconds(e.totalRequiredSeconds())).append(";")
          .append(formatSeconds(e.totalBalanceSeconds())).append("\n");
    });

    return "\uFEFF".getBytes(StandardCharsets.UTF_8);
  }
}
