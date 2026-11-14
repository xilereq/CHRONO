package pl.chrono.reportmanager.generator;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import pl.chrono.reportmanager.dto.ReportDocument;
import pl.chrono.reportmanager.dto.TeamNightHoursEntry;
import pl.chrono.reportmanager.dto.TeamNightHoursReport;

class TeamNightHoursReportGeneratorTest {

  private final TeamNightHoursReportGenerator generator = new TeamNightHoursReportGenerator();

  @Test
  void shouldListNightHoursPerEmployee() {
    LocalDate day = LocalDate.of(2026, 11, 2);
    TeamNightHoursReport report = new TeamNightHoursReport(1, day, day,
        List.of(new TeamNightHoursEntry(10, "Jan Kowalski", 3, 10800L)));

    ReportDocument document = generator.generate(report);

    assertThat(document.title()).isEqualTo("Raport Godzin Nocnych Zespołu");
    assertThat(document.formatRow(document.rows().get(0))).containsExactly("10", "Jan Kowalski", "3", "3:00");
  }
}
