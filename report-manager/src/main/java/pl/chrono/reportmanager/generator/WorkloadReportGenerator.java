package pl.chrono.reportmanager.generator;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.pdf.PdfPTable;
import java.util.List;
import org.apache.poi.ss.usermodel.Sheet;
import org.springframework.stereotype.Component;
import pl.chrono.reportmanager.dto.TeamWorkloadEntry;
import pl.chrono.reportmanager.dto.TeamWorkloadReport;

@Component
public class WorkloadReportGenerator extends AbstractReportGenerator<TeamWorkloadReport> {

  private static final String[] CSV_HEADERS = {
    "ID Pracownika", "Imie i Nazwisko", "Przepracowano", "Wymagano", "Bilans (Nadgodziny)"
  };
  private static final String[] PDF_HEADERS = {
    "ID", "Imie i Nazwisko", "Przepracowano", "Wymagano", "Bilans"
  };
  private static final String[] XLSX_HEADERS = {
    "ID Pracownika", "Imie i Nazwisko", "Przepracowano", "Wymagano", "Bilans"
  };

  @Override
  protected void writeCsv(TeamWorkloadReport report, StringBuilder csv) {
    csvRow(csv, "Raport Obciazenia Zespolu (Od - Do):", period(report));
    csvRow(csv);
    csvRow(csv, (Object[]) CSV_HEADERS);

    for (TeamWorkloadEntry entry : report.entries()) {
      csvRow(csv, rowValues(entry));
    }
  }

  @Override
  protected void writePdf(TeamWorkloadReport report, Document document) throws DocumentException {
    addPdfTitle(document, title(report));
    addPdfSpacer(document);

    PdfPTable table = createPdfTable(PDF_HEADERS);
    for (TeamWorkloadEntry entry : report.entries()) {
      addPdfRow(table, rowValues(entry));
    }
    document.add(table);
  }

  @Override
  protected String getSheetName() {
    return "Obciazenie Zespolu";
  }

  @Override
  protected void writeXlsx(TeamWorkloadReport report, Sheet sheet) {
    int rowIdx = writeXlsxHeader(sheet, List.of(title(report)), XLSX_HEADERS);
    for (TeamWorkloadEntry entry : report.entries()) {
      xlsxRow(sheet, rowIdx++, rowValues(entry));
    }
  }

  private String title(TeamWorkloadReport report) {
    return "Raport Obciazenia Zespolu: " + period(report);
  }

  private String period(TeamWorkloadReport report) {
    return report.startDate() + " - " + report.endDate();
  }

  private Object[] rowValues(TeamWorkloadEntry entry) {
    return new Object[] {
        entry.employeeId(),
        entry.employeeFullName(),
        formatSeconds(entry.totalWorkSeconds()),
        formatSeconds(entry.totalRequiredSeconds()),
        formatSeconds(entry.totalBalanceSeconds())
    };
  }
}
