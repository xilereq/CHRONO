package pl.chrono.reportmanager.generator;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import pl.chrono.reportmanager.dto.ReportFormat;
import pl.chrono.reportmanager.exception.ReportGenerationException;

public abstract class AbstractReportGenerator<T> implements I_ReportGenerator<T> {

  private static final String CSV_SEPARATOR = ";";

  private static final Font TITLE_FONT = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18);
  private static final Font NORMAL_FONT = FontFactory.getFont(FontFactory.HELVETICA, 12);
  private static final Font HEADER_FONT = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10);

  @Override
  public final byte[] generate(T report, ReportFormat format) {
    try {
      return switch (format) {
        case CSV -> generateCsv(report);
        case PDF -> generatePdf(report);
        case XLSX -> generateXlsx(report);
      };
    } catch (ReportGenerationException e) {
      throw e;
    } catch (Exception e) {
      throw new ReportGenerationException.Failed(String.valueOf(format), e);
    }
  }

  protected abstract void writeCsv(T report, StringBuilder csv);

  protected abstract void writePdf(T report, Document document) throws DocumentException;

  protected abstract String getSheetName();

  protected abstract void writeXlsx(T report, Sheet sheet);

  private byte[] generateCsv(T report) {
    StringBuilder csv = new StringBuilder();
    writeCsv(report, csv);
    return addUtf8Bom(csv.toString());
  }

  private byte[] generatePdf(T report) throws DocumentException {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    Document document = new Document();
    PdfWriter.getInstance(document, out);
    document.open();
    writePdf(report, document);
    document.close();
    return out.toByteArray();
  }

  private byte[] generateXlsx(T report) throws IOException {
    try (Workbook workbook = new XSSFWorkbook();
         ByteArrayOutputStream out = new ByteArrayOutputStream()) {
      Sheet sheet = workbook.createSheet(getSheetName());
      writeXlsx(report, sheet);
      autoSizeColumns(sheet);
      workbook.write(out);
      return out.toByteArray();
    }
  }

  // csvRow(csv) bez wartości dopisuje pustą linię
  protected void csvRow(StringBuilder csv, Object... values) {
    for (int i = 0; i < values.length; i++) {
      if (i > 0) {
        csv.append(CSV_SEPARATOR);
      }
      csv.append(values[i]);
    }
    csv.append('\n');
  }

  protected void addPdfTitle(Document document, String title) throws DocumentException {
    document.add(new Paragraph(title, TITLE_FONT));
  }

  protected void addPdfText(Document document, String text) throws DocumentException {
    document.add(new Paragraph(text, NORMAL_FONT));
  }

  protected void addPdfSpacer(Document document) throws DocumentException {
    document.add(new Paragraph(" "));
  }

  protected PdfPTable createPdfTable(String... headers) {
    PdfPTable table = new PdfPTable(headers.length);
    table.setWidthPercentage(100);
    for (String header : headers) {
      table.addCell(new PdfPCell(new Phrase(header, HEADER_FONT)));
    }
    return table;
  }

  protected void addPdfRow(PdfPTable table, Object... values) {
    for (Object value : values) {
      table.addCell(String.valueOf(value));
    }
  }

  // tytuł (linia na wiersz), pusty wiersz, nagłówki; zwraca indeks pierwszego wiersza danych
  protected int writeXlsxHeader(Sheet sheet, List<String> titleLines, String... headers) {
    for (int i = 0; i < titleLines.size(); i++) {
      sheet.createRow(i).createCell(0).setCellValue(titleLines.get(i));
    }
    int headerRowIdx = titleLines.size() + 1;
    xlsxRow(sheet, headerRowIdx, (Object[]) headers);
    return headerRowIdx + 1;
  }

  protected void xlsxRow(Sheet sheet, int rowIdx, Object... values) {
    Row row = sheet.createRow(rowIdx);
    for (int i = 0; i < values.length; i++) {
      if (values[i] instanceof Number number) {
        row.createCell(i).setCellValue(number.doubleValue());
      } else {
        row.createCell(i).setCellValue(String.valueOf(values[i]));
      }
    }
  }

  private void autoSizeColumns(Sheet sheet) {
    int columns = 0;
    for (Row row : sheet) {
      columns = Math.max(columns, row.getLastCellNum());
    }
    for (int i = 0; i < columns; i++) {
      sheet.autoSizeColumn(i);
    }
  }
}
