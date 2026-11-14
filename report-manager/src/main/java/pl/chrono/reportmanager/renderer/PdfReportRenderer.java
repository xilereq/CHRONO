package pl.chrono.reportmanager.renderer;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Font;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import org.springframework.stereotype.Component;
import pl.chrono.reportmanager.dto.ReportDocument;
import pl.chrono.reportmanager.dto.ReportField;
import pl.chrono.reportmanager.dto.ReportFormat;
import pl.chrono.reportmanager.exception.ReportGenerationException;

@Component
public class PdfReportRenderer implements I_ReportRenderer {

  private static final String REGULAR_FONT = "liberation/LiberationSans-Regular.ttf";
  private static final String BOLD_FONT = "liberation/LiberationSans-Bold.ttf";

  private final Font titleFont;
  private final Font textFont;
  private final Font headerFont;
  private final Font cellFont;

  public PdfReportRenderer() {
    BaseFont regular = loadFont(REGULAR_FONT);
    BaseFont bold = loadFont(BOLD_FONT);
    this.titleFont = new Font(bold, 18);
    this.textFont = new Font(regular, 12);
    this.headerFont = new Font(bold, 10);
    this.cellFont = new Font(regular, 10);
  }

  @Override
  public ReportFormat format() {
    return ReportFormat.PDF;
  }

  @Override
  public byte[] render(ReportDocument document) {
    try {
      ByteArrayOutputStream out = new ByteArrayOutputStream();
      Document pdf = new Document();
      PdfWriter.getInstance(pdf, out);
      pdf.open();
      pdf.add(new Paragraph(document.title(), titleFont));
      for (ReportField field : document.fields()) {
        pdf.add(new Paragraph(field.label() + ": " + field.formattedValue(), textFont));
      }
      pdf.add(new Paragraph(" "));
      pdf.add(createTable(document));
      pdf.close();
      return out.toByteArray();
    } catch (DocumentException e) {
      throw new ReportGenerationException.Failed(format().name(), e);
    }
  }

  private PdfPTable createTable(ReportDocument document) {
    PdfPTable table = new PdfPTable(document.columns().size());
    table.setWidthPercentage(100);
    for (String header : document.headers()) {
      table.addCell(new PdfPCell(new Phrase(header, headerFont)));
    }
    for (List<Object> row : document.rows()) {
      for (String value : document.formatRow(row)) {
        table.addCell(new PdfPCell(new Phrase(value, cellFont)));
      }
    }
    return table;
  }

  private static BaseFont loadFont(String path) {
    try {
      return BaseFont.createFont(path, BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
    } catch (DocumentException | IOException e) {
      throw new IllegalStateException("Cannot load PDF font " + path, e);
    }
  }
}
