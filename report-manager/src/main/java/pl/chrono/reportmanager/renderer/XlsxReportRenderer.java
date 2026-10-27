package pl.chrono.reportmanager.renderer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.WorkbookUtil;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;
import pl.chrono.reportmanager.dto.ReportDocument;
import pl.chrono.reportmanager.dto.ReportField;
import pl.chrono.reportmanager.dto.ReportFormat;
import pl.chrono.reportmanager.exception.ReportGenerationException;

@Component
public class XlsxReportRenderer implements I_ReportRenderer {

  @Override
  public ReportFormat format() {
    return ReportFormat.XLSX;
  }

  @Override
  public byte[] render(ReportDocument document) {
    try (Workbook workbook = new XSSFWorkbook();
         ByteArrayOutputStream out = new ByteArrayOutputStream()) {
      Sheet sheet = workbook.createSheet(WorkbookUtil.createSafeSheetName(document.title()));
      writeContent(sheet, document);
      for (int i = 0; i < document.columns().size(); i++) {
        sheet.autoSizeColumn(i);
      }
      workbook.write(out);
      return out.toByteArray();
    } catch (IOException e) {
      throw new ReportGenerationException.Failed(format().name(), e);
    }
  }

  private void writeContent(Sheet sheet, ReportDocument document) {
    int rowIndex = 0;
    writeRow(sheet.createRow(rowIndex++), List.of(document.title()));
    for (ReportField field : document.fields()) {
      writeRow(sheet.createRow(rowIndex++), List.of(field.label(), field.formattedValue()));
    }
    rowIndex++;
    writeRow(sheet.createRow(rowIndex++), document.headers());
    for (List<Object> row : document.rows()) {
      writeRow(sheet.createRow(rowIndex++), document.formatRow(row));
    }
  }

  private void writeRow(Row row, List<String> values) {
    for (int i = 0; i < values.size(); i++) {
      row.createCell(i).setCellValue(values.get(i));
    }
  }
}
