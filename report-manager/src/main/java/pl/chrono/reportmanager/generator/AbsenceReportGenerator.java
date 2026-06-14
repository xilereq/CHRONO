package pl.chrono.reportmanager.generator;

import org.springframework.stereotype.Component;
import pl.chrono.reportmanager.dto.ReportFormat;
import pl.chrono.reportmanager.dto.TeamAbsencesReport;
import java.nio.charset.StandardCharsets;

@Component
public class AbsenceReportGenerator implements I_ReportGenerator<TeamAbsencesReport> {

  @Override
  public byte[] generate(TeamAbsencesReport report, ReportFormat format) {
    return switch (format) {
      case CSV -> generateCsv(report);
      case PDF -> throw new UnsupportedOperationException("Eksport do PDF wymaga dodania biblioteki np. iText/OpenPDF");
      case XLSX -> throw new UnsupportedOperationException("Eksport do XLSX wymaga dodania biblioteki Apache POI");
    };
  }

  private byte[] generateCsv(TeamAbsencesReport report) {
    StringBuilder sb = new StringBuilder();
    sb.append("Raport Wnioskow i Absencji\n");
    sb.append("Od:;").append(report.startDate()).append("\n");
    sb.append("Do:;").append(report.endDate()).append("\n\n");

    sb.append("Pracownik;Typ Wniosku;Status;Data Od;Data Do\n");

    report.entries().forEach(e -> {
      sb.append(e.employeeFullName()).append(";")
          .append(e.applicationType()).append(";")
          .append(e.status()).append(";")
          .append(e.startDate()).append(";")
          .append(e.endDate()).append("\n");
    });

    return "\uFEFF".getBytes(StandardCharsets.UTF_8);
  }
}
