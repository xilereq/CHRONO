package pl.chrono.reportmanager.generator;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import pl.chrono.reportmanager.dto.ReportDocument;
import pl.chrono.reportmanager.dto.TeamOvertimeEntry;
import pl.chrono.reportmanager.dto.TeamOvertimeReport;

class TeamOvertimeReportGeneratorTest {

  private final TeamOvertimeReportGenerator generator = new TeamOvertimeReportGenerator();

  @Test
  void shouldBuildTeamOvertimeDocument() {
    LocalDate day = LocalDate.of(2026, 10, 1);
    TeamOvertimeEntry entry = new TeamOvertimeEntry(7, "Michał Anioł", 7200L);
    TeamOvertimeReport report = new TeamOvertimeReport(1, day, day, List.of(entry));

    ReportDocument document = generator.generate(report);

    assertThat(document.title()).isEqualTo("Zbiorczy Raport Nadgodzin Zespolu");
    assertThat(document.headers()).containsExactly("ID Pracownika", "Imie i Nazwisko", "Suma Nadgodzin");
    assertThat(document.formatRow(document.rows().get(0))).containsExactly("7", "Michał Anioł", "2:00");
  }
}
