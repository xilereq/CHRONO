package pl.chrono.reportmanager.generator;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.pdf.PdfPTable;
import java.util.List;
import org.apache.poi.ss.usermodel.Sheet;
import org.springframework.stereotype.Component;
import pl.chrono.reportmanager.dto.TeamAbsenceEntry;
import pl.chrono.reportmanager.dto.TeamAbsencesReport;

@Component
public class AbsenceReportGenerator extends AbstractReportGenerator<TeamAbsencesReport> {

  private static final String TITLE = "Raport Wnioskow i Absencji";
  private static final String[] HEADERS = {
    "Pracownik", "Typ Wniosku", "Status", "Data Od", "Data Do"
  };

  @Override
  protected void writeCsv(TeamAbsencesReport report, StringBuilder csv) {
    csvRow(csv, TITLE);
    csvRow(csv, "Od:", report.startDate());
    csvRow(csv, "Do:", report.endDate());
    csvRow(csv);
    csvRow(csv, (Object[]) HEADERS);

    for (TeamAbsenceEntry entry : report.entries()) {
      csvRow(csv, rowValues(entry));
    }
  }

  @Override
  protected void writePdf(TeamAbsencesReport report, Document document) throws DocumentException {
    addPdfTitle(document, TITLE);
    addPdfText(document, period(report));
    addPdfSpacer(document);

    PdfPTable table = createPdfTable(HEADERS);
    for (TeamAbsenceEntry entry : report.entries()) {
      addPdfRow(table, rowValues(entry));
    }
    document.add(table);
  }

  @Override
  protected String getSheetName() {
    return "Absencje";
  }

  @Override
  protected void writeXlsx(TeamAbsencesReport report, Sheet sheet) {
    int rowIdx = writeXlsxHeader(sheet, List.of(TITLE, period(report)), HEADERS);
    for (TeamAbsenceEntry entry : report.entries()) {
      xlsxRow(sheet, rowIdx++, rowValues(entry));
    }
  }

  private String period(TeamAbsencesReport report) {
    return "Od: " + report.startDate() + " Do: " + report.endDate();
  }

  private Object[] rowValues(TeamAbsenceEntry entry) {
    return new Object[] {
        entry.employeeFullName(),
        entry.applicationType(),
        entry.status(),
        entry.startDate(),
        entry.endDate()
    };
  }
}
