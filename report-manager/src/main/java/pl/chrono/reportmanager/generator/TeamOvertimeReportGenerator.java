package pl.chrono.reportmanager.generator;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.pdf.PdfPTable;
import java.util.List;
import org.apache.poi.ss.usermodel.Sheet;
import org.springframework.stereotype.Component;
import pl.chrono.reportmanager.dto.TeamOvertimeEntry;
import pl.chrono.reportmanager.dto.TeamOvertimeReport;

@Component
public class TeamOvertimeReportGenerator extends AbstractReportGenerator<TeamOvertimeReport> {

  private static final String[] HEADERS = {"ID Pracownika", "Imie i Nazwisko", "Suma Nadgodzin"};

  @Override
  protected void writeCsv(TeamOvertimeReport report, StringBuilder csv) {
    csvRow(csv, "Zbiorczy Raport Nadgodzin Zespolu (Od - Do):", period(report));
    csvRow(csv);
    csvRow(csv, (Object[]) HEADERS);

    for (TeamOvertimeEntry entry : report.entries()) {
      csvRow(csv, rowValues(entry));
    }
  }

  @Override
  protected void writePdf(TeamOvertimeReport report, Document document) throws DocumentException {
    addPdfTitle(document, title(report));
    addPdfSpacer(document);

    PdfPTable table = createPdfTable(HEADERS);
    for (TeamOvertimeEntry entry : report.entries()) {
      addPdfRow(table, rowValues(entry));
    }
    document.add(table);
  }

  @Override
  protected String getSheetName() {
    return "Nadgodziny - Zespol";
  }

  @Override
  protected void writeXlsx(TeamOvertimeReport report, Sheet sheet) {
    int rowIdx = writeXlsxHeader(sheet, List.of(title(report)), HEADERS);
    for (TeamOvertimeEntry entry : report.entries()) {
      xlsxRow(sheet, rowIdx++, rowValues(entry));
    }
  }

  private String period(TeamOvertimeReport report) {
    return report.startDate() + " - " + report.endDate();
  }

  private String title(TeamOvertimeReport report) {
    return "Zbiorczy Raport Nadgodzin Zespolu: " + period(report);
  }

  private Object[] rowValues(TeamOvertimeEntry entry) {
    return new Object[] {
        entry.employeeId(),
        entry.employeeFullName(),
        formatSeconds(entry.totalOvertimeSeconds())
    };
  }
}
