package pl.chrono.reportmanager.generator;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import pl.chrono.reportmanager.dto.ReportDocument;
import pl.chrono.reportmanager.dto.TeamAbsenceEntry;
import pl.chrono.reportmanager.dto.TeamAbsencesReport;

class AbsenceReportGeneratorTest {

  private final AbsenceReportGenerator generator = new AbsenceReportGenerator();

  @Test
  void shouldBuildAbsenceDocument() {
    LocalDate start = LocalDate.of(2026, 10, 5);
    LocalDate end = LocalDate.of(2026, 10, 9);
    TeamAbsenceEntry entry = new TeamAbsenceEntry(1, "Jan Kowalski", "HOLIDAY_LEAVE", "APPROVED", start, end);
    TeamAbsencesReport report = new TeamAbsencesReport(1, start, end, List.of(entry));

    ReportDocument document = generator.generate(report);

    assertThat(document.title()).isEqualTo("Raport Wnioskow i Absencji");
    assertThat(document.formatRow(document.rows().get(0)))
        .containsExactly("Jan Kowalski", "HOLIDAY_LEAVE", "APPROVED", "2026-10-05", "2026-10-09");
  }
}
