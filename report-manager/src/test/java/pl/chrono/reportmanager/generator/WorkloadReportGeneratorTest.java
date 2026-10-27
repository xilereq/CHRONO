package pl.chrono.reportmanager.generator;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import pl.chrono.reportmanager.dto.ReportDocument;
import pl.chrono.reportmanager.dto.TeamWorkloadEntry;
import pl.chrono.reportmanager.dto.TeamWorkloadReport;

class WorkloadReportGeneratorTest {

  private final WorkloadReportGenerator generator = new WorkloadReportGenerator();

  @Test
  void shouldBuildWorkloadDocument() {
    LocalDate day = LocalDate.of(2026, 10, 1);
    TeamWorkloadEntry entry = new TeamWorkloadEntry(3, "Janina Kowal", 144000L, 144000L, 0L);
    TeamWorkloadReport report = new TeamWorkloadReport(1, day, day, List.of(entry));

    ReportDocument document = generator.generate(report);

    assertThat(document.title()).isEqualTo("Raport Obciazenia Zespolu");
    assertThat(document.formatRow(document.rows().get(0)))
        .containsExactly("3", "Janina Kowal", "40:00", "40:00", "0:00");
  }
}
