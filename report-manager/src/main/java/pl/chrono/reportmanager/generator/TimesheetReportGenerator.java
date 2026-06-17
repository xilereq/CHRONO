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
import pl.chrono.reportmanager.dto.EmployeeTimesheetReport;
import pl.chrono.reportmanager.dto.DailyTimesheetEntry;
import pl.chrono.reportmanager.dto.ReportFormat;
import pl.chrono.reportmanager.exception.ReportGenerationException;

import java.io.ByteArrayOutputStream;

@Component
public class TimesheetReportGenerator implements I_ReportGenerator<EmployeeTimesheetReport> {

  @Override
  public byte[] generate(EmployeeTimesheetReport report, ReportFormat format) {
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

  private byte[] generateCsv(EmployeeTimesheetReport report) {
    StringBuilder sb = new StringBuilder();
    sb.append("Raport Okresowy:;").append(report.startDate()).append(" do ").append(report.endDate()).append("\n");
    sb.append("Pracownik:;").append(report.employeeFullName()).append("\n");
    sb.append("Przepracowano:;").append(formatSeconds(report.totalWorkSeconds())).append("\n");
    sb.append("Wymagano:;").append(formatSeconds(report.totalRequiredSeconds())).append("\n");
    sb.append("Bilans:;").append(formatSeconds(report.totalBalanceSeconds())).append("\n");
    sb.append("Godziny Nocne:;").append(formatSeconds(report.totalNightSeconds())).append("\n\n");

    sb.append("Data;Start;Koniec;Efektywny Czas;Wymagany Czas;Bilans;Godziny Nocne;Wniosek\n");

    for (DailyTimesheetEntry entry : report.dailyEntries()) {
      sb.append(entry.date()).append(";");
      sb.append(formatTime(entry.startTime())).append(";");
      sb.append(formatTime(entry.endTime())).append(";");
      sb.append(formatSeconds(entry.effectiveWorkSeconds())).append(";");
      sb.append(formatSeconds((long) entry.requiredSeconds())).append(";");
      sb.append(formatSeconds(entry.balanceSeconds())).append(";");
      sb.append(formatSeconds(entry.nightSeconds())).append(";");
      sb.append(entry.applicationType() != null ? entry.applicationType() : "").append("\n");
    }

    return addUtf8Bom(sb.toString());
  }

  private byte[] generatePdf(EmployeeTimesheetReport report) throws Exception {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    Document document = new Document();
    PdfWriter.getInstance(document, out);
    document.open();

    Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18);
    Font normalFont = FontFactory.getFont(FontFactory.HELVETICA, 12);
    Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10);

    document.add(new Paragraph("Raport Okresowy: " + report.startDate() + " do " + report.endDate(), titleFont));
    document.add(new Paragraph("Pracownik: " + report.employeeFullName(), normalFont));
    document.add(new Paragraph("Przepracowano: " + formatSeconds(report.totalWorkSeconds()), normalFont));
    document.add(new Paragraph("Wymagano: " + formatSeconds(report.totalRequiredSeconds()), normalFont));
    document.add(new Paragraph("Bilans: " + formatSeconds(report.totalBalanceSeconds()), normalFont));
    document.add(new Paragraph("Godziny Nocne: " + formatSeconds(report.totalNightSeconds()), normalFont));
    document.add(new Paragraph(" "));

    PdfPTable table = new PdfPTable(8);
    table.setWidthPercentage(100);

    String[] headers = {"Data", "Start", "Koniec", "Efekt. Czas", "Wymagano", "Bilans", "Nocne", "Wniosek"};
    for (String header : headers) {
      table.addCell(new PdfPCell(new Phrase(header, headerFont)));
    }

    for (DailyTimesheetEntry entry : report.dailyEntries()) {
      table.addCell(entry.date().toString());
      table.addCell(formatTime(entry.startTime()));
      table.addCell(formatTime(entry.endTime()));
      table.addCell(formatSeconds(entry.effectiveWorkSeconds()));
      table.addCell(formatSeconds((long) entry.requiredSeconds()));
      table.addCell(formatSeconds(entry.balanceSeconds()));
      table.addCell(formatSeconds(entry.nightSeconds()));
      table.addCell(entry.applicationType() != null ? entry.applicationType() : "");
    }

    document.add(table);
    document.close();
    return out.toByteArray();
  }

  private byte[] generateXlsx(EmployeeTimesheetReport report) throws Exception {
    try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
      Sheet sheet = workbook.createSheet("Timesheet");

      sheet.createRow(0).createCell(0).setCellValue("Raport Okresowy: " + report.startDate() + " do " + report.endDate());
      sheet.createRow(1).createCell(0).setCellValue("Pracownik: " + report.employeeFullName());
      sheet.createRow(2).createCell(0).setCellValue("Przepracowano: " + formatSeconds(report.totalWorkSeconds()));
      sheet.createRow(3).createCell(0).setCellValue("Wymagano: " + formatSeconds(report.totalRequiredSeconds()));
      sheet.createRow(4).createCell(0).setCellValue("Bilans: " + formatSeconds(report.totalBalanceSeconds()));
      sheet.createRow(5).createCell(0).setCellValue("Godziny Nocne: " + formatSeconds(report.totalNightSeconds()));

      Row headerRow = sheet.createRow(7);
      String[] headers = {"Data", "Start", "Koniec", "Efektywny Czas", "Wymagany Czas", "Bilans", "Godziny Nocne", "Wniosek"};
      for (int i = 0; i < headers.length; i++) {
        headerRow.createCell(i).setCellValue(headers[i]);
      }

      int rowIdx = 8;
      for (DailyTimesheetEntry entry : report.dailyEntries()) {
        Row row = sheet.createRow(rowIdx++);
        row.createCell(0).setCellValue(entry.date().toString());
        row.createCell(1).setCellValue(formatTime(entry.startTime()));
        row.createCell(2).setCellValue(formatTime(entry.endTime()));
        row.createCell(3).setCellValue(formatSeconds(entry.effectiveWorkSeconds()));
        row.createCell(4).setCellValue(formatSeconds((long) entry.requiredSeconds()));
        row.createCell(5).setCellValue(formatSeconds(entry.balanceSeconds()));
        row.createCell(6).setCellValue(formatSeconds(entry.nightSeconds()));
        row.createCell(7).setCellValue(entry.applicationType() != null ? entry.applicationType() : "");
      }

      for (int i = 0; i < headers.length; i++) {
        sheet.autoSizeColumn(i);
      }

      workbook.write(out);
      return out.toByteArray();
    }
  }
}
