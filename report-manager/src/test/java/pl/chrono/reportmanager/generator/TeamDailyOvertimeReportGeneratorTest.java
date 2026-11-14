package pl.chrono.reportmanager.generator;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import pl.chrono.reportmanager.dto.ReportDocument;
import pl.chrono.reportmanager.dto.ReportField;
import pl.chrono.reportmanager.dto.TeamDailyOvertimeEntry;
import pl.chrono.reportmanager.dto.TeamDailyOvertimeReport;

class TeamDailyOvertimeReportGeneratorTest {

  private final TeamDailyOvertimeReportGenerator generator = new TeamDailyOvertimeReportGenerator();

  @Test
  void shouldListOvertimePerDayWithTotal() {
    LocalDate day1 = LocalDate.of(2026, 11, 2);
    LocalDate day2 = LocalDate.of(2026, 11, 3);
    TeamDailyOvertimeReport report = new TeamDailyOvertimeReport(1, day1, day2, List.of(
        new TeamDailyOvertimeEntry(day1, 2, 5400L),
        new TeamDailyOvertimeEntry(day2, 0, 0L)));

    ReportDocument document = generator.generate(report);

    assertThat(document.title()).isEqualTo("Nadgodziny Zespołu wg Dnia");
    assertThat(document.fields()).extracting(ReportField::formattedValue)
        .containsExactly("2026-11-02 - 2026-11-03", "1:30");
    assertThat(document.formatRow(document.rows().get(0))).containsExactly("2026-11-02", "2", "1:30");
    assertThat(document.formatRow(document.rows().get(1))).containsExactly("2026-11-03", "0", "0:00");
  }
}
