package pl.chrono.reportmanager.generator;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.pdf.PdfPTable;
import java.util.ArrayList;
import java.util.List;
import org.apache.poi.ss.usermodel.Sheet;
import org.springframework.stereotype.Component;
import pl.chrono.reportmanager.dto.DailyTimesheetEntry;
import pl.chrono.reportmanager.dto.EmployeeTimesheetReport;

@Component
public class TimesheetReportGenerator extends AbstractReportGenerator<EmployeeTimesheetReport> {

  private static final String[] HEADERS = {
    "Data", "Start", "Koniec", "Efektywny Czas", "Wymagany Czas", "Bilans", "Godziny Nocne",
    "Wniosek"
  };
  private static final String[] PDF_HEADERS = {
    "Data", "Start", "Koniec", "Efekt. Czas", "Wymagano", "Bilans", "Nocne", "Wniosek"
  };

  @Override
  protected void writeCsv(EmployeeTimesheetReport report, StringBuilder csv) {
    csvRow(csv, "Raport Okresowy:", report.startDate() + " do " + report.endDate());
    csvRow(csv, "Pracownik:", report.employeeFullName());
    csvRow(csv, "Przepracowano:", formatSeconds(report.totalWorkSeconds()));
    csvRow(csv, "Wymagano:", formatSeconds(report.totalRequiredSeconds()));
    csvRow(csv, "Bilans:", formatSeconds(report.totalBalanceSeconds()));
    csvRow(csv, "Godziny Nocne:", formatSeconds(report.totalNightSeconds()));
    csvRow(csv);
    csvRow(csv, (Object[]) HEADERS);

    for (DailyTimesheetEntry entry : report.dailyEntries()) {
      csvRow(csv, rowValues(entry));
    }
  }

  @Override
  protected void writePdf(EmployeeTimesheetReport report, Document document)
      throws DocumentException {
    addPdfTitle(document, title(report));
    for (String line : summaryLines(report)) {
      addPdfText(document, line);
    }
    addPdfSpacer(document);

    PdfPTable table = createPdfTable(PDF_HEADERS);
    for (DailyTimesheetEntry entry : report.dailyEntries()) {
      addPdfRow(table, rowValues(entry));
    }
    document.add(table);
  }

  @Override
  protected String getSheetName() {
    return "Timesheet";
  }

  @Override
  protected void writeXlsx(EmployeeTimesheetReport report, Sheet sheet) {
    List<String> titleLines = new ArrayList<>();
    titleLines.add(title(report));
    titleLines.addAll(summaryLines(report));

    int rowIdx = writeXlsxHeader(sheet, titleLines, HEADERS);
    for (DailyTimesheetEntry entry : report.dailyEntries()) {
      xlsxRow(sheet, rowIdx++, rowValues(entry));
    }
  }

  private String title(EmployeeTimesheetReport report) {
    return "Raport Okresowy: " + report.startDate() + " do " + report.endDate();
  }

  private List<String> summaryLines(EmployeeTimesheetReport report) {
    return List.of(
        "Pracownik: " + report.employeeFullName(),
        "Przepracowano: " + formatSeconds(report.totalWorkSeconds()),
        "Wymagano: " + formatSeconds(report.totalRequiredSeconds()),
        "Bilans: " + formatSeconds(report.totalBalanceSeconds()),
        "Godziny Nocne: " + formatSeconds(report.totalNightSeconds())
    );
  }

  private Object[] rowValues(DailyTimesheetEntry entry) {
    return new Object[] {
        entry.date(),
        formatTime(entry.startTime()),
        formatTime(entry.endTime()),
        formatSeconds(entry.effectiveWorkSeconds()),
        formatSeconds((long) entry.requiredSeconds()),
        formatSeconds(entry.balanceSeconds()),
        formatSeconds(entry.nightSeconds()),
        entry.applicationType() != null ? entry.applicationType() : ""
    };
  }
}
