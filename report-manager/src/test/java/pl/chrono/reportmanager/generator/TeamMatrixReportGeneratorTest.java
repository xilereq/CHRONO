package pl.chrono.reportmanager.generator;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import pl.chrono.reportmanager.dto.ReportDocument;
import pl.chrono.reportmanager.dto.TeamMatrixDay;
import pl.chrono.reportmanager.dto.TeamMatrixReport;

class TeamMatrixReportGeneratorTest {

  private final TeamMatrixReportGenerator generator = new TeamMatrixReportGenerator();

  @Test
  void shouldUseEmployeesAsColumnsAndDaysAsRows() {
    LocalDate day = LocalDate.of(2026, 11, 2);
    TeamMatrixReport report = new TeamMatrixReport(1, day, day,
        List.of("Jan Kowalski", "Anna Nowak"),
        List.of(new TeamMatrixDay(day, List.of(28800L, 30600L))));

    ReportDocument document = generator.generate(report);

    assertThat(document.title()).isEqualTo("Miesięczna Macierz Zespołu");
    assertThat(document.headers()).containsExactly("Data", "Jan Kowalski", "Anna Nowak");
    assertThat(document.formatRow(document.rows().get(0))).containsExactly("2026-11-02", "8:00", "8:30");
  }
}
