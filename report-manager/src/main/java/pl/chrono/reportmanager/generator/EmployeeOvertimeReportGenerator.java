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
import pl.chrono.reportmanager.dto.EmployeeOvertimeReport;
import pl.chrono.reportmanager.dto.OvertimeEntry;
import pl.chrono.reportmanager.dto.ReportFormat;
import pl.chrono.reportmanager.exception.ReportGenerationException;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

@Component
public class EmployeeOvertimeReportGenerator implements I_ReportGenerator<EmployeeOvertimeReport> {

  @Override
  public byte[] generate(EmployeeOvertimeReport report, ReportFormat format) {
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

  private byte[] generatePdf(EmployeeOvertimeReport report) throws Exception {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    Document document = new Document();
    PdfWriter.getInstance(document, out);
    document.open();

    Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18);
    Font normalFont = FontFactory.getFont(FontFactory.HELVETICA, 12);
    Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10);

    document.add(new Paragraph("Raport Nadgodzin: " + report.startDate() + " - " + report.endDate(), titleFont));
    document.add(new Paragraph("Pracownik: " + report.employeeFullName(), normalFont));
    document.add(new Paragraph("Suma nadgodzin: " + formatSeconds(report.totalOvertimeSeconds()), normalFont));
    document.add(new Paragraph(" "));

    PdfPTable table = new PdfPTable(4);
    table.setWidthPercentage(100);

    String[] headers = {"Data", "Przepracowano", "Wymagano", "Nadgodziny"};
    for (String header : headers) {
      table.addCell(new PdfPCell(new Phrase(header, headerFont)));
    }

    for (OvertimeEntry e : report.entries()) {
      table.addCell(e.date().toString());
      table.addCell(formatSeconds(e.workedSeconds()));
      table.addCell(formatSeconds((long) e.requiredSeconds()));
      table.addCell(formatSeconds(e.overtimeSeconds()));
    }

    document.add(table);
    document.close();
    return out.toByteArray();
  }

  private byte[] generateXlsx(EmployeeOvertimeReport report) throws Exception {
    try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
      Sheet sheet = workbook.createSheet("Nadgodziny - Pracownik");

      sheet.createRow(0).createCell(0).setCellValue("Raport Nadgodzin: " + report.startDate() + " - " + report.endDate());
      sheet.createRow(1).createCell(0).setCellValue("Pracownik: " + report.employeeFullName());
      sheet.createRow(2).createCell(0).setCellValue("Suma nadgodzin: " + formatSeconds(report.totalOvertimeSeconds()));

      Row headerRow = sheet.createRow(4);
      String[] headers = {"Data", "Przepracowano", "Wymagano", "Nadgodziny"};
      for (int i = 0; i < headers.length; i++) {
        headerRow.createCell(i).setCellValue(headers[i]);
      }

      int rowIdx = 5;
      for (OvertimeEntry e : report.entries()) {
        Row row = sheet.createRow(rowIdx++);
        row.createCell(0).setCellValue(e.date().toString());
        row.createCell(1).setCellValue(formatSeconds(e.workedSeconds()));
        row.createCell(2).setCellValue(formatSeconds((long) e.requiredSeconds()));
        row.createCell(3).setCellValue(formatSeconds(e.overtimeSeconds()));
      }

      for (int i = 0; i < headers.length; i++) {
        sheet.autoSizeColumn(i);
      }

      workbook.write(out);
      return out.toByteArray();
    }
  }
}
