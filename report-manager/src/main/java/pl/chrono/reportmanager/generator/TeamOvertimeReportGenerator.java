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
import pl.chrono.reportmanager.dto.TeamOvertimeEntry;
import pl.chrono.reportmanager.dto.TeamOvertimeReport;
import pl.chrono.reportmanager.exception.ReportGenerationException;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

@Component
public class TeamOvertimeReportGenerator implements I_ReportGenerator<TeamOvertimeReport> {

  @Override
  public byte[] generate(TeamOvertimeReport report, ReportFormat format) {
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

  private byte[] generatePdf(TeamOvertimeReport report) throws Exception {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    Document document = new Document();
    PdfWriter.getInstance(document, out);
    document.open();

    Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18);
    Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10);

    document.add(new Paragraph("Zbiorczy Raport Nadgodzin Zespolu: " + report.startDate() + " - " + report.endDate(), titleFont));
    document.add(new Paragraph(" "));

    PdfPTable table = new PdfPTable(3);
    table.setWidthPercentage(100);

    String[] headers = {"ID Pracownika", "Imie i Nazwisko", "Suma Nadgodzin"};
    for (String header : headers) {
      table.addCell(new PdfPCell(new Phrase(header, headerFont)));
    }

    for (TeamOvertimeEntry e : report.entries()) {
      table.addCell(String.valueOf(e.employeeId()));
      table.addCell(e.employeeFullName());
      table.addCell(formatSeconds(e.totalOvertimeSeconds()));
    }

    document.add(table);
    document.close();
    return out.toByteArray();
  }

  private byte[] generateXlsx(TeamOvertimeReport report) throws Exception {
    try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
      Sheet sheet = workbook.createSheet("Nadgodziny - Zespol");

      sheet.createRow(0).createCell(0).setCellValue("Zbiorczy Raport Nadgodzin Zespolu: " + report.startDate() + " - " + report.endDate());

      Row headerRow = sheet.createRow(2);
      String[] headers = {"ID Pracownika", "Imie i Nazwisko", "Suma Nadgodzin"};
      for (int i = 0; i < headers.length; i++) {
        headerRow.createCell(i).setCellValue(headers[i]);
      }

      int rowIdx = 3;
      for (TeamOvertimeEntry e : report.entries()) {
        Row row = sheet.createRow(rowIdx++);
        row.createCell(0).setCellValue(e.employeeId());
        row.createCell(1).setCellValue(e.employeeFullName());
        row.createCell(2).setCellValue(formatSeconds(e.totalOvertimeSeconds()));
      }

      for (int i = 0; i < headers.length; i++) {
        sheet.autoSizeColumn(i);
      }

      workbook.write(out);
      return out.toByteArray();
    }
  }
}
