package pl.chrono.reportmanager.renderer;

import pl.chrono.reportmanager.dto.ReportDocument;
import pl.chrono.reportmanager.dto.ReportFormat;

public interface I_ReportRenderer {

  ReportFormat format();

  byte[] render(ReportDocument document);
}
