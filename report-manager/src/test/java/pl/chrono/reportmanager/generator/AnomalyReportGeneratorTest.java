package pl.chrono.reportmanager.generator;

import org.junit.jupiter.api.Test;
import pl.chrono.reportmanager.dto.ReportFormat;
import pl.chrono.reportmanager.dto.TeamAnomalyEntry;
import pl.chrono.reportmanager.dto.TeamAnomalyReport;
import pl.chrono.reportmanager.exception.ReportGenerationException;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AnomalyReportGeneratorTest {

  private final AnomalyReportGenerator generator = new AnomalyReportGenerator();

  @Test
  void shouldGenerateCsv() {
    TeamAnomalyEntry entry = new TeamAnomalyEntry(1, "Anna Nowak", 36000L, 28800L, 7200L, "ZNACZNE NADGODZINY");
    TeamAnomalyReport report = new TeamAnomalyReport(1, LocalDate.now(), LocalDate.now(), List.of(entry));

    byte[] result = generator.generate(report, ReportFormat.CSV);
    String content = new String(result, StandardCharsets.UTF_8);

    assertThat(result).isNotEmpty();
    assertThat(content).contains("Anna Nowak;10:00;8:00;2:00;ZNACZNE NADGODZINY");
  }

  @Test
  void shouldGeneratePdf() {
    TeamAnomalyEntry entry = new TeamAnomalyEntry(1, "Anna Nowak", 36000L, 28800L, 7200L, "ZNACZNE NADGODZINY");
    TeamAnomalyReport report = new TeamAnomalyReport(1, LocalDate.now(), LocalDate.now(), List.of(entry));

    byte[] result = generator.generate(report, ReportFormat.PDF);

    assertThat(result).isNotEmpty();
    assertThat(new String(result).substring(0, 5)).isEqualTo("%PDF-");
  }

  @Test
  void shouldGenerateXlsx() {
    TeamAnomalyEntry entry = new TeamAnomalyEntry(1, "Anna Nowak", 36000L, 28800L, 7200L, "ZNACZNE NADGODZINY");
    TeamAnomalyReport report = new TeamAnomalyReport(1, LocalDate.now(), LocalDate.now(), List.of(entry));

    byte[] result = generator.generate(report, ReportFormat.XLSX);

    assertThat(result).isNotEmpty();
  }

  @Test
  void shouldThrowExceptionOnGenerationFailure() {
    assertThatThrownBy(() -> generator.generate(null, ReportFormat.CSV))
        .isInstanceOf(ReportGenerationException.Failed.class);
  }
}
