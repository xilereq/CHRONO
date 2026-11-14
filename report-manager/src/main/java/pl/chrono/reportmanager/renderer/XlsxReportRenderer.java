package pl.chrono.reportmanager.renderer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.ss.util.WorkbookUtil;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;
import pl.chrono.reportmanager.dto.ReportDocument;
import pl.chrono.reportmanager.dto.ReportField;
import pl.chrono.reportmanager.dto.ReportFormat;
import pl.chrono.reportmanager.dto.ValueType;
import pl.chrono.reportmanager.exception.ReportGenerationException;

@Component
public class XlsxReportRenderer implements I_ReportRenderer {

  private static final double SECONDS_PER_DAY = 86_400.0;

  @Override
  public ReportFormat format() {
    return ReportFormat.XLSX;
  }

  @Override
  public byte[] render(ReportDocument document) {
    try (XSSFWorkbook workbook = new XSSFWorkbook();
         ByteArrayOutputStream out = new ByteArrayOutputStream()) {
      workbook.getCTWorkbook().getWorkbookPr().setDate1904(true);
      Sheet sheet = workbook.createSheet(WorkbookUtil.createSafeSheetName(document.title()));
      writeContent(workbook, sheet, document);
      for (int i = 0; i < document.columns().size(); i++) {
        sheet.autoSizeColumn(i);
      }
      workbook.write(out);
      return out.toByteArray();
    } catch (IOException e) {
      throw new ReportGenerationException.Failed(format().name(), e);
    }
  }

  private void writeContent(Workbook workbook, Sheet sheet, ReportDocument document) {
    Map<ValueType, CellStyle> valueStyles = createValueStyles(workbook);
    int lastColumn = document.columns().size() - 1;

    writeTitle(workbook, sheet, document.title(), lastColumn);

    int rowIndex = 1;
    for (ReportField field : document.fields()) {
      Row row = sheet.createRow(rowIndex++);
      row.createCell(0).setCellValue(field.label());
      writeValue(row.createCell(1), field.value(), field.type(), valueStyles);
    }

    int headerRowIndex = rowIndex + 1;
    writeHeader(workbook, sheet.createRow(headerRowIndex), document.headers());

    rowIndex = headerRowIndex + 1;
    for (List<Object> values : document.rows()) {
      Row row = sheet.createRow(rowIndex++);
      for (int i = 0; i < values.size(); i++) {
        writeValue(row.createCell(i), values.get(i), document.columns().get(i).type(), valueStyles);
      }
    }

    sheet.createFreezePane(0, headerRowIndex + 1);
    sheet.setAutoFilter(new CellRangeAddress(headerRowIndex, rowIndex - 1, 0, lastColumn));
  }

  private void writeTitle(Workbook workbook, Sheet sheet, String title, int lastColumn) {
    Font font = workbook.createFont();
    font.setBold(true);
    font.setFontHeightInPoints((short) 14);
    CellStyle style = workbook.createCellStyle();
    style.setFont(font);

    Cell cell = sheet.createRow(0).createCell(0);
    cell.setCellValue(title);
    cell.setCellStyle(style);
    if (lastColumn > 0) {
      sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, lastColumn));
    }
  }

  private void writeHeader(Workbook workbook, Row row, List<String> headers) {
    Font font = workbook.createFont();
    font.setBold(true);
    CellStyle style = workbook.createCellStyle();
    style.setFont(font);
    style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
    style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
    style.setBorderBottom(BorderStyle.THIN);

    for (int i = 0; i < headers.size(); i++) {
      Cell cell = row.createCell(i);
      cell.setCellValue(headers.get(i));
      cell.setCellStyle(style);
    }
  }

  private Map<ValueType, CellStyle> createValueStyles(Workbook workbook) {
    Map<ValueType, CellStyle> styles = new EnumMap<>(ValueType.class);
    for (ValueType type : ValueType.values()) {
      CellStyle style = workbook.createCellStyle();
      style.setDataFormat(workbook.createDataFormat().getFormat(excelFormat(type)));
      styles.put(type, style);
    }
    return styles;
  }

  private String excelFormat(ValueType type) {
    return switch (type) {
      case DURATION -> "[h]:mm";
      case TIME -> "hh:mm";
      case DATE -> "yyyy-mm-dd";
      case NUMBER -> "0";
      case TEXT -> "@";
    };
  }

  private void writeValue(Cell cell, Object value, ValueType type,
                          Map<ValueType, CellStyle> styles) {
    cell.setCellStyle(styles.get(type));
    if (value == null) {
      return;
    }
    switch (type) {
      case DURATION -> cell.setCellValue(((Number) value).longValue() / SECONDS_PER_DAY);
      case TIME -> cell.setCellValue(((LocalTime) value).toSecondOfDay() / SECONDS_PER_DAY);
      case DATE -> cell.setCellValue((LocalDate) value);
      case NUMBER -> cell.setCellValue(((Number) value).doubleValue());
      default -> cell.setCellValue(value.toString());
    }
  }
}
