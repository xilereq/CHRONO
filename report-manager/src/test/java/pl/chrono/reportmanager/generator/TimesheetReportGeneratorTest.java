package pl.chrono.reportmanager.generator;

import org.junit.jupiter.api.Test;
import pl.chrono.reportmanager.dto.DailyTimesheetEntry;
import pl.chrono.reportmanager.dto.EmployeeTimesheetReport;
import pl.chrono.reportmanager.dto.ReportFormat;
import pl.chrono.reportmanager.exception.ReportGenerationException;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TimesheetReportGeneratorTest {

  private final TimesheetReportGenerator generator = new TimesheetReportGenerator();

  @Test
  void shouldGenerateCsv() {
    DailyTimesheetEntry entry1 = new DailyTimesheetEntry(LocalDate.now(), LocalTime.of(8, 0), LocalTime.of(16, 0), 28800L, 28800, 0L, 0L, null);
    DailyTimesheetEntry entry2 = new DailyTimesheetEntry(LocalDate.now().plusDays(1), null, null, 28800L, 28800, 0L, 0L, "HOLIDAY");
    EmployeeTimesheetReport report = new EmployeeTimesheetReport(1, "Test User", LocalDate.now(), LocalDate.now().plusDays(1), 57600L, 57600L, 0L, 0L, List.of(entry1, entry2));

    byte[] result = generator.generate(report, ReportFormat.CSV);
    String content = new String(result, StandardCharsets.UTF_8);

    assertThat(result).isNotEmpty();
    assertThat(content).contains("Test User");
    assertThat(content).contains("08:00;16:00;8:00;8:00;0:00;0:00;");
    assertThat(content).contains("-;-;8:00;8:00;0:00;0:00;HOLIDAY");
  }

  @Test
  void shouldGeneratePdf() {
    DailyTimesheetEntry entry = new DailyTimesheetEntry(LocalDate.now(), LocalTime.of(8, 0), LocalTime.of(16, 0), 28800L, 28800, 0L, 0L, null);
    EmployeeTimesheetReport report = new EmployeeTimesheetReport(1, "Test User", LocalDate.now(), LocalDate.now(), 28800L, 28800L, 0L, 0L, List.of(entry));

    byte[] result = generator.generate(report, ReportFormat.PDF);

    assertThat(result).isNotEmpty();
    assertThat(new String(result).substring(0, 5)).isEqualTo("%PDF-");
  }

  @Test
  void shouldGenerateXlsx() {
    DailyTimesheetEntry entry = new DailyTimesheetEntry(LocalDate.now(), LocalTime.of(8, 0), LocalTime.of(16, 0), 28800L, 28800, 0L, 0L, null);
    EmployeeTimesheetReport report = new EmployeeTimesheetReport(1, "Test User", LocalDate.now(), LocalDate.now(), 28800L, 28800L, 0L, 0L, List.of(entry));

    byte[] result = generator.generate(report, ReportFormat.XLSX);

    assertThat(result).isNotEmpty();
  }

  @Test
  void shouldThrowExceptionOnGenerationFailure() {
    assertThatThrownBy(() -> generator.generate(null, ReportFormat.CSV))
        .isInstanceOf(ReportGenerationException.Failed.class);
  }
}
