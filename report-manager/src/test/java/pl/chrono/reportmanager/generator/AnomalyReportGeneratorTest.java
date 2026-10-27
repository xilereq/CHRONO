package pl.chrono.reportmanager.generator;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import pl.chrono.reportmanager.dto.ReportDocument;
import pl.chrono.reportmanager.dto.TeamAnomalyEntry;
import pl.chrono.reportmanager.dto.TeamAnomalyReport;

class AnomalyReportGeneratorTest {

  private final AnomalyReportGenerator generator = new AnomalyReportGenerator();

  @Test
  void shouldBuildAnomalyDocument() {
    LocalDate day = LocalDate.of(2026, 10, 1);
    TeamAnomalyEntry entry =
        new TeamAnomalyEntry(1, "Anna Nowak", 36000L, 28800L, 7200L, "ZNACZNE NADGODZINY");
    TeamAnomalyReport report = new TeamAnomalyReport(1, day, day, List.of(entry));

    ReportDocument document = generator.generate(report);

    assertThat(document.title()).isEqualTo("Raport Anomalii Czasu Pracy");
    assertThat(document.formatRow(document.rows().get(0)))
        .containsExactly("Anna Nowak", "10:00", "8:00", "2:00", "ZNACZNE NADGODZINY");
  }
}
