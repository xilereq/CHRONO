package pl.chrono.reportmanager.renderer;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import pl.chrono.reportmanager.dto.ReportColumn;
import pl.chrono.reportmanager.dto.ReportDocument;
import pl.chrono.reportmanager.dto.ReportField;
import pl.chrono.reportmanager.dto.ValueType;

class CsvReportRendererTest {

  private final CsvReportRenderer renderer = new CsvReportRenderer();

  @Test
  void shouldRenderFieldsHeadersAndRowsWithBom() {
    ReportDocument document = new ReportDocument(
        "Raport",
        List.of(new ReportField("Okres", "2026-10-01 - 2026-10-31", ValueType.TEXT)),
        List.of(new ReportColumn("Pracownik", ValueType.TEXT), new ReportColumn("Czas", ValueType.DURATION)),
        List.of(Arrays.asList("Jan Kowalski", 5400L)));

    String csv = new String(renderer.render(document), StandardCharsets.UTF_8);

    assertThat(csv).isEqualTo(
        "﻿Okres;2026-10-01 - 2026-10-31\n\nPracownik;Czas\nJan Kowalski;1:30\n");
  }
}
