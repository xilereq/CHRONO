package pl.chrono.reportmanager.renderer;

import static org.assertj.core.api.Assertions.assertThat;

import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.parser.PdfTextExtractor;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import pl.chrono.reportmanager.dto.ReportColumn;
import pl.chrono.reportmanager.dto.ReportDocument;
import pl.chrono.reportmanager.dto.ReportField;
import pl.chrono.reportmanager.dto.ValueType;

class PdfReportRendererTest {

  private final PdfReportRenderer renderer = new PdfReportRenderer();

  @Test
  void shouldRenderTitleFieldsAndTable() throws IOException {
    ReportDocument document = new ReportDocument(
        "Raport Testowy",
        List.of(new ReportField("Okres", "2026-10-01 - 2026-10-31", ValueType.TEXT)),
        List.of(new ReportColumn("Pracownik", ValueType.TEXT), new ReportColumn("Czas", ValueType.DURATION)),
        List.of(Arrays.asList("Jan Kowalski", 5400L)));

    byte[] pdf = renderer.render(document);
    String text = new PdfTextExtractor(new PdfReader(pdf)).getTextFromPage(1);

    assertThat(new String(pdf, 0, 5)).isEqualTo("%PDF-");
    assertThat(text).contains("Raport Testowy", "Okres: 2026-10-01 - 2026-10-31", "Jan Kowalski", "1:30");
  }
}
