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
import pl.chrono.reportmanager.dto.TeamAbsenceEntry;
import pl.chrono.reportmanager.dto.TeamAbsencesReport;
import pl.chrono.reportmanager.exception.ReportGenerationException;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

@Component
public class AbsenceReportGenerator implements I_ReportGenerator<TeamAbsencesReport> {

  @Override
  public byte[] generate(TeamAbsencesReport report, ReportFormat format) {
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

  private byte[] generatePdf(TeamAbsencesReport report) throws Exception {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    Document document = new Document();
    PdfWriter.getInstance(document, out);
    document.open();

    Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18);
    Font normalFont = FontFactory.getFont(FontFactory.HELVETICA, 12);
    Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10);

    document.add(new Paragraph("Raport Wnioskow i Absencji", titleFont));
    document.add(new Paragraph("Od: " + report.startDate() + " Do: " + report.endDate(), normalFont));
    document.add(new Paragraph(" "));

    PdfPTable table = new PdfPTable(5);
    table.setWidthPercentage(100);

    String[] headers = {"Pracownik", "Typ Wniosku", "Status", "Data Od", "Data Do"};
    for (String header : headers) {
      table.addCell(new PdfPCell(new Phrase(header, headerFont)));
    }

    for (TeamAbsenceEntry e : report.entries()) {
      table.addCell(e.employeeFullName());
      table.addCell(e.applicationType());
      table.addCell(e.status());
      table.addCell(e.startDate().toString());
      table.addCell(e.endDate().toString());
    }

    document.add(table);
    document.close();
    return out.toByteArray();
  }
}
