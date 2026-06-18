package pl.chrono.reportmanager.generator;

import org.junit.jupiter.api.Test;
import pl.chrono.reportmanager.dto.EmployeeOvertimeReport;
import pl.chrono.reportmanager.dto.OvertimeEntry;
import pl.chrono.reportmanager.dto.ReportFormat;
import pl.chrono.reportmanager.exception.ReportGenerationException;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EmployeeOvertimeReportGeneratorTest {

  private final EmployeeOvertimeReportGenerator generator = new EmployeeOvertimeReportGenerator();

  @Test
  void shouldGenerateCsv() {
    OvertimeEntry entry = new OvertimeEntry(LocalDate.now(), 32400L, 28800, 3600L);
    EmployeeOvertimeReport report = new EmployeeOvertimeReport(1, "Piotr Testowy", LocalDate.now(), LocalDate.now(), 3600L, List.of(entry));

    byte[] result = generator.generate(report, ReportFormat.CSV);
    String content = new String(result, StandardCharsets.UTF_8);

    assertThat(result).isNotEmpty();
    assertThat(content).contains("Piotr Testowy");
    assertThat(content).contains("9:00;8:00;1:00");
  }

  @Test
  void shouldGeneratePdf() {
    OvertimeEntry entry = new OvertimeEntry(LocalDate.now(), 32400L, 28800, 3600L);
    EmployeeOvertimeReport report = new EmployeeOvertimeReport(1, "Piotr Testowy", LocalDate.now(), LocalDate.now(), 3600L, List.of(entry));

    byte[] result = generator.generate(report, ReportFormat.PDF);

    assertThat(result).isNotEmpty();
    assertThat(new String(result).substring(0, 5)).isEqualTo("%PDF-");
  }

  @Test
  void shouldGenerateXlsx() {
    OvertimeEntry entry = new OvertimeEntry(LocalDate.now(), 32400L, 28800, 3600L);
    EmployeeOvertimeReport report = new EmployeeOvertimeReport(1, "Piotr Testowy", LocalDate.now(), LocalDate.now(), 3600L, List.of(entry));

    byte[] result = generator.generate(report, ReportFormat.XLSX);

    assertThat(result).isNotEmpty();
  }

  @Test
  void shouldThrowExceptionOnGenerationFailure() {
    assertThatThrownBy(() -> generator.generate(null, ReportFormat.CSV))
        .isInstanceOf(ReportGenerationException.Failed.class);
  }
}
