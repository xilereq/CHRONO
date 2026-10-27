package pl.chrono.reportmanager.renderer;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;
import pl.chrono.reportmanager.dto.ReportDocument;
import pl.chrono.reportmanager.dto.ReportFormat;
import pl.chrono.reportmanager.exception.ReportGenerationException;

@Component
public class ReportExporter {

  private final Map<ReportFormat, I_ReportRenderer> renderers;

  public ReportExporter(List<I_ReportRenderer> renderers) {
    this.renderers = renderers.stream()
        .collect(Collectors.toMap(I_ReportRenderer::format, Function.identity()));
  }

  public byte[] export(ReportDocument document, ReportFormat format) {
    I_ReportRenderer renderer = renderers.get(format);
    if (renderer == null) {
      throw new ReportGenerationException.UnsupportedFormat(String.valueOf(format));
    }
    return renderer.render(document);
  }
}
