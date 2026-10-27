package pl.chrono.reportmanager.generator;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.pdf.PdfPTable;
import java.util.List;
import org.apache.poi.ss.usermodel.Sheet;
import org.springframework.stereotype.Component;
import pl.chrono.reportmanager.dto.TeamAnomalyEntry;
import pl.chrono.reportmanager.dto.TeamAnomalyReport;

@Component
public class AnomalyReportGenerator extends AbstractReportGenerator<TeamAnomalyReport> {

  private static final String[] HEADERS = {
    "Pracownik", "Przepracowano", "Wymagano", "Bilans", "Typ Anomalii"
  };

  @Override
  protected void writeCsv(TeamAnomalyReport report, StringBuilder csv) {
    csvRow(csv, "Raport Anomalii Czasu Pracy:", period(report));
    csvRow(csv);
    csvRow(csv, (Object[]) HEADERS);

    for (TeamAnomalyEntry entry : report.entries()) {
      csvRow(csv, rowValues(entry));
    }
  }

  @Override
  protected void writePdf(TeamAnomalyReport report, Document document) throws DocumentException {
    addPdfTitle(document, "Raport Anomalii Czasu Pracy: " + period(report));
    addPdfSpacer(document);

    PdfPTable table = createPdfTable(HEADERS);
    for (TeamAnomalyEntry entry : report.entries()) {
      addPdfRow(table, rowValues(entry));
    }
    document.add(table);
  }

  @Override
  protected String getSheetName() {
    return "Anomalie";
  }

  @Override
  protected void writeXlsx(TeamAnomalyReport report, Sheet sheet) {
    int rowIdx = writeXlsxHeader(sheet, List.of("Raport Anomalii: " + period(report)), HEADERS);
    for (TeamAnomalyEntry entry : report.entries()) {
      xlsxRow(sheet, rowIdx++, rowValues(entry));
    }
  }

  private String period(TeamAnomalyReport report) {
    return report.startDate() + " - " + report.endDate();
  }

  private Object[] rowValues(TeamAnomalyEntry entry) {
    return new Object[] {
        entry.employeeFullName(),
        formatSeconds(entry.totalWorkSeconds()),
        formatSeconds(entry.totalRequiredSeconds()),
        formatSeconds(entry.balanceSeconds()),
        entry.anomalyType()
    };
  }
}
