package pl.chrono.reportmanager.generator;

import org.junit.jupiter.api.Test;
import pl.chrono.reportmanager.dto.ReportFormat;
import pl.chrono.reportmanager.dto.TeamAbsenceEntry;
import pl.chrono.reportmanager.dto.TeamAbsencesReport;
import pl.chrono.reportmanager.exception.ReportGenerationException;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AbsenceReportGeneratorTest {

  private final AbsenceReportGenerator generator = new AbsenceReportGenerator();

  @Test
  void shouldGenerateCsv() {
    TeamAbsenceEntry entry = new TeamAbsenceEntry(1, "Jan Kowalski", "HOLIDAY", "APPROVED", LocalDate.now(), LocalDate.now());
    TeamAbsencesReport report = new TeamAbsencesReport(1, LocalDate.now(), LocalDate.now(), List.of(entry));

    byte[] result = generator.generate(report, ReportFormat.CSV);
    String content = new String(result, StandardCharsets.UTF_8);

    assertThat(result).isNotEmpty();
    assertThat(content).contains("Jan Kowalski;HOLIDAY;APPROVED");
  }

  @Test
  void shouldGeneratePdf() {
    TeamAbsenceEntry entry = new TeamAbsenceEntry(1, "Jan Kowalski", "HOLIDAY", "APPROVED", LocalDate.now(), LocalDate.now());
    TeamAbsencesReport report = new TeamAbsencesReport(1, LocalDate.now(), LocalDate.now(), List.of(entry));

    byte[] result = generator.generate(report, ReportFormat.PDF);

    assertThat(result).isNotEmpty();
    assertThat(new String(result).substring(0, 5)).isEqualTo("%PDF-");
  }

  @Test
  void shouldGenerateXlsx() {
    TeamAbsenceEntry entry = new TeamAbsenceEntry(1, "Jan Kowalski", "HOLIDAY", "APPROVED", LocalDate.now(), LocalDate.now());
    TeamAbsencesReport report = new TeamAbsencesReport(1, LocalDate.now(), LocalDate.now(), List.of(entry));

    byte[] result = generator.generate(report, ReportFormat.XLSX);

    assertThat(result).isNotEmpty();
    assertThat(result.length).isGreaterThan(100);
  }

  @Test
  void shouldThrowExceptionOnGenerationFailure() {
    assertThatThrownBy(() -> generator.generate(null, ReportFormat.CSV))
        .isInstanceOf(ReportGenerationException.Failed.class);
  }
}
