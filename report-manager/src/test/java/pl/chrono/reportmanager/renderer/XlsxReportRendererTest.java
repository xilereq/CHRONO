package pl.chrono.reportmanager.renderer;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import pl.chrono.reportmanager.dto.ReportColumn;
import pl.chrono.reportmanager.dto.ReportDocument;
import pl.chrono.reportmanager.dto.ReportField;
import pl.chrono.reportmanager.dto.ValueType;

class XlsxReportRendererTest {

  private final XlsxReportRenderer renderer = new XlsxReportRenderer();

  @Test
  void shouldRenderTitleFieldsHeadersAndRows() throws IOException {
    ReportDocument document = new ReportDocument(
        "Raport Testowy",
        List.of(new ReportField("Okres", "2026-10-01 - 2026-10-31", ValueType.TEXT)),
        List.of(new ReportColumn("Pracownik", ValueType.TEXT), new ReportColumn("Czas", ValueType.DURATION)),
        List.of(Arrays.asList("Jan Kowalski", 5400L)));

    try (Workbook workbook = new XSSFWorkbook(new ByteArrayInputStream(renderer.render(document)))) {
      Sheet sheet = workbook.getSheetAt(0);

      assertThat(sheet.getSheetName()).isEqualTo("Raport Testowy");
      assertThat(sheet.getRow(0).getCell(0).getStringCellValue()).isEqualTo("Raport Testowy");
      assertThat(sheet.getRow(1).getCell(1).getStringCellValue()).isEqualTo("2026-10-01 - 2026-10-31");
      assertThat(sheet.getRow(3).getCell(0).getStringCellValue()).isEqualTo("Pracownik");
      assertThat(sheet.getRow(4).getCell(0).getStringCellValue()).isEqualTo("Jan Kowalski");
      assertThat(sheet.getRow(4).getCell(1).getStringCellValue()).isEqualTo("1:30");
    }
  }
}
