package pl.chrono.reportmanager.generator;

import org.junit.jupiter.api.Test;
import pl.chrono.reportmanager.dto.ReportFormat;
import pl.chrono.reportmanager.dto.TeamWorkloadEntry;
import pl.chrono.reportmanager.dto.TeamWorkloadReport;
import pl.chrono.reportmanager.exception.ReportGenerationException;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WorkloadReportGeneratorTest {

  private final WorkloadReportGenerator generator = new WorkloadReportGenerator();

  @Test
  void shouldGenerateCsv() {
    TeamWorkloadEntry entry = new TeamWorkloadEntry(1, "Janina Kowal", 144000L, 144000L, 0L);
    TeamWorkloadReport report = new TeamWorkloadReport(1, LocalDate.now(), LocalDate.now(), List.of(entry));

    byte[] result = generator.generate(report, ReportFormat.CSV);
    String content = new String(result, StandardCharsets.UTF_8);

    assertThat(result).isNotEmpty();
    assertThat(content).contains("Janina Kowal;40:00;40:00;0:00");
  }

  @Test
  void shouldGeneratePdf() {
    TeamWorkloadEntry entry = new TeamWorkloadEntry(1, "Janina Kowal", 144000L, 144000L, 0L);
    TeamWorkloadReport report = new TeamWorkloadReport(1, LocalDate.now(), LocalDate.now(), List.of(entry));

    byte[] result = generator.generate(report, ReportFormat.PDF);

    assertThat(result).isNotEmpty();
    assertThat(new String(result).substring(0, 5)).isEqualTo("%PDF-");
  }

  @Test
  void shouldGenerateXlsx() {
    TeamWorkloadEntry entry = new TeamWorkloadEntry(1, "Janina Kowal", 144000L, 144000L, 0L);
    TeamWorkloadReport report = new TeamWorkloadReport(1, LocalDate.now(), LocalDate.now(), List.of(entry));

    byte[] result = generator.generate(report, ReportFormat.XLSX);

    assertThat(result).isNotEmpty();
  }

  @Test
  void shouldThrowExceptionOnGenerationFailure() {
    assertThatThrownBy(() -> generator.generate(null, ReportFormat.CSV))
        .isInstanceOf(ReportGenerationException.Failed.class);
  }
}
