package pl.chrono.reportmanager.generator;

import com.lowagie.text.Document;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.stereotype.Component;
import pl.chrono.reportmanager.dto.ReportFormat;
import pl.chrono.reportmanager.dto.TeamWorkloadEntry;
import pl.chrono.reportmanager.dto.TeamWorkloadReport;
import pl.chrono.reportmanager.exception.ReportGenerationException;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

@Component
public class WorkloadReportGenerator implements I_ReportGenerator<TeamWorkloadReport> {

  @Override
  public byte[] generate(TeamWorkloadReport report, ReportFormat format) {
    try {
      return switch (format) {
        case CSV -> generateCsv(report);
        case PDF -> generatePdf(report);
        case XLSX -> throw new ReportGenerationException.UnsupportedFormat(format.name());
      };
    } catch (ReportGenerationException e) {
      throw e;
    } catch (Exception e) {
      throw new ReportGenerationException.Failed(format.name(), e);
    }
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

  private byte[] generatePdf(TeamWorkloadReport report) throws Exception {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    Document document = new Document();
    PdfWriter.getInstance(document, out);
    document.open();

    Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18);
    Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10);

    document.add(new Paragraph("Raport Obciazenia Zespolu: " + report.startDate() + " - " + report.endDate(), titleFont));
    document.add(new Paragraph(" "));

    PdfPTable table = new PdfPTable(5);
    table.setWidthPercentage(100);

    String[] headers = {"ID", "Imie i Nazwisko", "Przepracowano", "Wymagano", "Bilans"};
    for (String header : headers) {
      table.addCell(new PdfPCell(new Phrase(header, headerFont)));
    }

    for (TeamWorkloadEntry e : report.entries()) {
      table.addCell(String.valueOf(e.employeeId()));
      table.addCell(e.employeeFullName());
      table.addCell(formatSeconds(e.totalWorkSeconds()));
      table.addCell(formatSeconds(e.totalRequiredSeconds()));
      table.addCell(formatSeconds(e.totalBalanceSeconds()));
    }

    document.add(table);
    document.close();
    return out.toByteArray();
  }
}
