package pl.chrono.reportmanager.renderer;

import java.nio.charset.StandardCharsets;
import java.util.List;
import org.springframework.stereotype.Component;
import pl.chrono.reportmanager.dto.ReportDocument;
import pl.chrono.reportmanager.dto.ReportField;
import pl.chrono.reportmanager.dto.ReportFormat;

@Component
public class CsvReportRenderer implements I_ReportRenderer {

  private static final String UTF8_BOM = "﻿";
  private static final String SEPARATOR = ";";

  @Override
  public ReportFormat format() {
    return ReportFormat.CSV;
  }

  @Override
  public byte[] render(ReportDocument document) {
    StringBuilder csv = new StringBuilder(UTF8_BOM);
    for (ReportField field : document.fields()) {
      appendLine(csv, List.of(field.label(), field.formattedValue()));
    }
    csv.append('\n');
    appendLine(csv, document.headers());
    for (List<Object> row : document.rows()) {
      appendLine(csv, document.formatRow(row));
    }
    return csv.toString().getBytes(StandardCharsets.UTF_8);
  }

  private void appendLine(StringBuilder csv, List<String> values) {
    csv.append(String.join(SEPARATOR, values)).append('\n');
  }
}
