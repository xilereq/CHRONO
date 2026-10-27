package pl.chrono.reportmanager.generator;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import pl.chrono.reportmanager.dto.DailyTimesheetEntry;
import pl.chrono.reportmanager.dto.EmployeeTimesheetReport;
import pl.chrono.reportmanager.dto.ReportDocument;
import pl.chrono.reportmanager.dto.ReportField;
import pl.chrono.reportmanager.renderer.CsvReportRenderer;

class TimesheetReportGeneratorTest {

  private static final LocalDate DAY = LocalDate.of(2026, 10, 1);

  private final TimesheetReportGenerator generator = new TimesheetReportGenerator();

  @Test
  void shouldBuildSummaryAndDailyRows() {
    ReportDocument document = generator.generate(sampleReport());

    assertThat(document.title()).isEqualTo("Raport Okresowy");
    assertThat(document.fields()).extracting(ReportField::formattedValue)
        .containsExactly("2026-10-01 - 2026-10-02", "Test User", "16:00", "16:00", "0:00", "1:00");
    assertThat(document.formatRow(document.rows().get(0)))
        .containsExactly("2026-10-01", "05:00", "13:00", "8:00", "8:00", "0:00", "1:00", "");
    assertThat(document.formatRow(document.rows().get(1)))
        .containsExactly("2026-10-02", "-", "-", "8:00", "8:00", "0:00", "0:00", "HOLIDAY_LEAVE");
  }

  @Test
  void shouldKeepCsvLayoutReadByFrontend() {
    byte[] csv = new CsvReportRenderer().render(generator.generate(sampleReport()));
    String[] lines = new String(csv, StandardCharsets.UTF_8).substring(1).split("\n");

    assertThat(lines[2]).isEqualTo("Przepracowano;16:00");
    assertThat(lines[3]).isEqualTo("Wymagano;16:00");
    assertThat(lines[4]).isEqualTo("Bilans;0:00");
    assertThat(lines[5]).isEqualTo("Godziny Nocne;1:00");
    assertThat(lines[7]).startsWith("Data;");
    assertThat(lines[8]).isEqualTo("2026-10-01;05:00;13:00;8:00;8:00;0:00;1:00;");
  }

  private EmployeeTimesheetReport sampleReport() {
    DailyTimesheetEntry worked = new DailyTimesheetEntry(
        DAY, LocalTime.of(5, 0), LocalTime.of(13, 0), 28800L, 28800, 0L, 3600L, null);
    DailyTimesheetEntry holiday = new DailyTimesheetEntry(
        DAY.plusDays(1), null, null, 28800L, 28800, 0L, 0L, "HOLIDAY_LEAVE");
    return new EmployeeTimesheetReport(
        1, "Test User", DAY, DAY.plusDays(1), 57600L, 57600L, 0L, 3600L, List.of(worked, holiday));
  }
}
