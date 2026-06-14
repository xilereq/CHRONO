package pl.chrono.reportmanager.generator;

import com.lowagie.text.Document;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;
import pl.chrono.reportmanager.dto.ReportFormat;
import pl.chrono.reportmanager.dto.TeamAbsenceEntry;
import pl.chrono.reportmanager.dto.TeamAbsencesReport;
import pl.chrono.reportmanager.exception.ReportGenerationException;

import java.io.ByteArrayOutputStream;

@Component
public class AbsenceReportGenerator implements I_ReportGenerator<TeamAbsencesReport> {

  @Override
  public byte[] generate(TeamAbsencesReport report, ReportFormat format) {
    try {
      return switch (format) {
        case CSV -> generateCsv(report);
        case PDF -> generatePdf(report);
        case XLSX -> generateXlsx(report);
        default -> throw new ReportGenerationException.UnsupportedFormat(format.name());
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

    return addUtf8Bom(sb.toString());
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

  private byte[] generateXlsx(TeamAbsencesReport report) throws Exception {
    try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
      Sheet sheet = workbook.createSheet("Absencje");

      sheet.createRow(0).createCell(0).setCellValue("Raport Wnioskow i Absencji");
      sheet.createRow(1).createCell(0).setCellValue("Od: " + report.startDate() + " Do: " + report.endDate());

      Row headerRow = sheet.createRow(3);
      String[] headers = {"Pracownik", "Typ Wniosku", "Status", "Data Od", "Data Do"};
      for (int i = 0; i < headers.length; i++) {
        headerRow.createCell(i).setCellValue(headers[i]);
      }

      int rowIdx = 4;
      for (TeamAbsenceEntry e : report.entries()) {
        Row row = sheet.createRow(rowIdx++);
        row.createCell(0).setCellValue(e.employeeFullName());
        row.createCell(1).setCellValue(e.applicationType());
        row.createCell(2).setCellValue(e.status());
        row.createCell(3).setCellValue(e.startDate().toString());
        row.createCell(4).setCellValue(e.endDate().toString());
      }

      for (int i = 0; i < headers.length; i++) {
        sheet.autoSizeColumn(i);
      }

      workbook.write(out);
      return out.toByteArray();
    }
  }
}
