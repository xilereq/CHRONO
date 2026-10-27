package pl.chrono.reportmanager.renderer;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import java.io.ByteArrayOutputStream;
import java.util.List;
import org.springframework.stereotype.Component;
import pl.chrono.reportmanager.dto.ReportDocument;
import pl.chrono.reportmanager.dto.ReportField;
import pl.chrono.reportmanager.dto.ReportFormat;
import pl.chrono.reportmanager.exception.ReportGenerationException;

@Component
public class PdfReportRenderer implements I_ReportRenderer {

  private static final Font TITLE_FONT = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18);
  private static final Font TEXT_FONT = FontFactory.getFont(FontFactory.HELVETICA, 12);
  private static final Font HEADER_FONT = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10);

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
      pdf.add(new Paragraph(document.title(), TITLE_FONT));
      for (ReportField field : document.fields()) {
        pdf.add(new Paragraph(field.label() + ": " + field.formattedValue(), TEXT_FONT));
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
      table.addCell(new PdfPCell(new Phrase(header, HEADER_FONT)));
    }
    for (List<Object> row : document.rows()) {
      document.formatRow(row).forEach(table::addCell);
    }
    return table;
  }
}
