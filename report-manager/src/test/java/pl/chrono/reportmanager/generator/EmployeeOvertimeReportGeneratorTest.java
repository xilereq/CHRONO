package pl.chrono.reportmanager.generator;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import pl.chrono.reportmanager.dto.EmployeeOvertimeReport;
import pl.chrono.reportmanager.dto.OvertimeEntry;
import pl.chrono.reportmanager.dto.ReportDocument;
import pl.chrono.reportmanager.dto.ReportField;

class EmployeeOvertimeReportGeneratorTest {

  private final EmployeeOvertimeReportGenerator generator = new EmployeeOvertimeReportGenerator();

  @Test
  void shouldBuildOvertimeDocument() {
    LocalDate day = LocalDate.of(2026, 10, 1);
    OvertimeEntry entry = new OvertimeEntry(day, 32400L, 28800, 3600L);
    EmployeeOvertimeReport report =
        new EmployeeOvertimeReport(1, "Piotr Testowy", day, day, 3600L, List.of(entry));

    ReportDocument document = generator.generate(report);

    assertThat(document.title()).isEqualTo("Raport Nadgodzin");
    assertThat(document.fields()).extracting(ReportField::formattedValue)
        .containsExactly("2026-10-01 - 2026-10-01", "Piotr Testowy", "1:00");
    assertThat(document.formatRow(document.rows().get(0)))
        .containsExactly("2026-10-01", "9:00", "8:00", "1:00");
  }
}
