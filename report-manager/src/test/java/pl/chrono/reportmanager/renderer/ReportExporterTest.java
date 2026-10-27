package pl.chrono.reportmanager.renderer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.Test;
import pl.chrono.reportmanager.dto.ReportDocument;
import pl.chrono.reportmanager.dto.ReportFormat;
import pl.chrono.reportmanager.exception.ReportGenerationException;

class ReportExporterTest {

  private final CsvReportRenderer csvRenderer = new CsvReportRenderer();
  private final ReportExporter exporter = new ReportExporter(List.of(csvRenderer));
  private final ReportDocument document = new ReportDocument("Raport", List.of(), List.of(), List.of());

  @Test
  void shouldUseRendererMatchingFormat() {
    assertThat(exporter.export(document, ReportFormat.CSV)).isEqualTo(csvRenderer.render(document));
  }

  @Test
  void shouldRejectFormatWithoutRenderer() {
    assertThatThrownBy(() -> exporter.export(document, ReportFormat.PDF))
        .isInstanceOf(ReportGenerationException.UnsupportedFormat.class);
  }
}
