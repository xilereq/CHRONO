package pl.chrono.reportmanager.generator;

import org.springframework.stereotype.Component;
import pl.chrono.reportmanager.dto.ReportFormat;
import pl.chrono.reportmanager.dto.TeamAbsencesReport;
import pl.chrono.reportmanager.exception.ReportGenerationException;
import java.nio.charset.StandardCharsets;

@Component
public class AbsenceReportGenerator implements I_ReportGenerator<TeamAbsencesReport> {

  @Override
  public byte[] generate(TeamAbsencesReport report, ReportFormat format) {
    return switch (format) {
      case CSV -> generateCsv(report);
      case PDF -> throw new ReportGenerationException.UnsupportedFormat(format.name());
      case XLSX -> throw new ReportGenerationException.UnsupportedFormat(format.name());
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
