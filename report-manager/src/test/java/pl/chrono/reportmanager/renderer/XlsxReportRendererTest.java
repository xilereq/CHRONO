package pl.chrono.reportmanager.renderer;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import pl.chrono.reportmanager.dto.ReportColumn;
import pl.chrono.reportmanager.dto.ReportDocument;
import pl.chrono.reportmanager.dto.ReportField;
import pl.chrono.reportmanager.dto.ValueType;

class XlsxReportRendererTest {

  private static final double SECONDS_PER_DAY = 86_400.0;
  private static final int HEADER_ROW = 3;
  private static final int FIRST_DATA_ROW = 4;

  private final XlsxReportRenderer renderer = new XlsxReportRenderer();

  private XSSFWorkbook workbook;
  private Sheet sheet;

  @BeforeEach
  void renderSampleDocument() throws IOException {
    ReportDocument document = new ReportDocument(
        "Raport Obciążenia Zespołu",
        List.of(new ReportField("Bilans", -28800L, ValueType.DURATION)),
        List.of(
            new ReportColumn("ID", ValueType.NUMBER),
            new ReportColumn("Data", ValueType.DATE),
            new ReportColumn("Start", ValueType.TIME),
            new ReportColumn("Bilans", ValueType.DURATION),
            new ReportColumn("Wniosek", ValueType.TEXT)),
        List.of(Arrays.asList(7, LocalDate.of(2026, 10, 1), LocalTime.of(8, 30), -5400L, null)));

    workbook = new XSSFWorkbook(new ByteArrayInputStream(renderer.render(document)));
    sheet = workbook.getSheetAt(0);
  }

  @Test
  void shouldUse1904DateSystemSoNegativeDurationsAreDisplayed() {
    assertThat(workbook.isDate1904()).isTrue();
    assertThat(sheet.getRow(1).getCell(1).getNumericCellValue()).isEqualTo(-28800 / SECONDS_PER_DAY);
  }

  @Test
  void shouldWriteTypedValuesWithFormats() {
    Row row = sheet.getRow(FIRST_DATA_ROW);

    assertThat(row.getCell(0).getNumericCellValue()).isEqualTo(7);
    assertThat(row.getCell(1).getLocalDateTimeCellValue().toLocalDate()).isEqualTo(LocalDate.of(2026, 10, 1));
    assertThat(row.getCell(1).getCellStyle().getDataFormatString()).isEqualTo("yyyy-mm-dd");
    assertThat(row.getCell(2).getNumericCellValue()).isEqualTo(30600 / SECONDS_PER_DAY);
    assertThat(row.getCell(2).getCellStyle().getDataFormatString()).isEqualTo("hh:mm");
    assertThat(row.getCell(3).getNumericCellValue()).isEqualTo(-5400 / SECONDS_PER_DAY);
    assertThat(row.getCell(3).getCellStyle().getDataFormatString()).isEqualTo("[h]:mm");
    assertThat(row.getCell(4).getCellType()).isEqualTo(CellType.BLANK);
  }

  @Test
  void shouldStyleTitleAndHeaderAndFreezeHeader() {
    Cell title = sheet.getRow(0).getCell(0);
    Cell header = sheet.getRow(HEADER_ROW).getCell(0);

    assertThat(title.getStringCellValue()).isEqualTo("Raport Obciążenia Zespołu");
    assertThat(workbook.getFontAt(title.getCellStyle().getFontIndex()).getBold()).isTrue();
    assertThat(header.getStringCellValue()).isEqualTo("ID");
    assertThat(workbook.getFontAt(header.getCellStyle().getFontIndex()).getBold()).isTrue();
    assertThat(sheet.getPaneInformation().getHorizontalSplitPosition()).isEqualTo((short) FIRST_DATA_ROW);
  }
}
