package pl.chrono.reportmanager.generator;

import org.junit.jupiter.api.Test;
import pl.chrono.reportmanager.dto.ReportFormat;
import pl.chrono.reportmanager.dto.TeamOvertimeEntry;
import pl.chrono.reportmanager.dto.TeamOvertimeReport;
import pl.chrono.reportmanager.exception.ReportGenerationException;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TeamOvertimeReportGeneratorTest {

  private final TeamOvertimeReportGenerator generator = new TeamOvertimeReportGenerator();

  @Test
  void shouldGenerateCsv() {
    TeamOvertimeEntry entry = new TeamOvertimeEntry(1, "Michał Anioł", 7200L);
    TeamOvertimeReport report = new TeamOvertimeReport(1, LocalDate.now(), LocalDate.now(), List.of(entry));

    byte[] result = generator.generate(report, ReportFormat.CSV);
    String content = new String(result, StandardCharsets.UTF_8);

    assertThat(result).isNotEmpty();
    assertThat(content).contains("Michał Anioł;2:00");
  }

  @Test
  void shouldGeneratePdf() {
    TeamOvertimeEntry entry = new TeamOvertimeEntry(1, "Michał Anioł", 7200L);
    TeamOvertimeReport report = new TeamOvertimeReport(1, LocalDate.now(), LocalDate.now(), List.of(entry));

    byte[] result = generator.generate(report, ReportFormat.PDF);

    assertThat(result).isNotEmpty();
    assertThat(new String(result).substring(0, 5)).isEqualTo("%PDF-");
  }

  @Test
  void shouldGenerateXlsx() {
    TeamOvertimeEntry entry = new TeamOvertimeEntry(1, "Michał Anioł", 7200L);
    TeamOvertimeReport report = new TeamOvertimeReport(1, LocalDate.now(), LocalDate.now(), List.of(entry));

    byte[] result = generator.generate(report, ReportFormat.XLSX);

    assertThat(result).isNotEmpty();
  }

  @Test
  void shouldThrowExceptionOnGenerationFailure() {
    assertThatThrownBy(() -> generator.generate(null, ReportFormat.CSV))
        .isInstanceOf(ReportGenerationException.Failed.class);
  }
}
