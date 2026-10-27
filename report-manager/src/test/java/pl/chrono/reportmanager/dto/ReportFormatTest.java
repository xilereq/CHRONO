package pl.chrono.reportmanager.dto;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ReportFormatTest {

  @Test
  void shouldBuildFileNameWithExtension() {
    assertThat(ReportFormat.CSV.fileName("team_workload_2026-10-01_to_2026-10-31"))
        .isEqualTo("team_workload_2026-10-01_to_2026-10-31.csv");
    assertThat(ReportFormat.PDF.fileName("raport")).isEqualTo("raport.pdf");
    assertThat(ReportFormat.XLSX.fileName("raport")).isEqualTo("raport.xlsx");
  }

  @Test
  void shouldExposeMediaTypes() {
    assertThat(ReportFormat.CSV.getMediaType()).isEqualTo("text/csv; charset=UTF-8");
    assertThat(ReportFormat.PDF.getMediaType()).isEqualTo("application/pdf");
    assertThat(ReportFormat.XLSX.getMediaType())
        .isEqualTo("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
  }
}
