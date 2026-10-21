package pl.chrono.reportmanager.generator;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.pdf.PdfPTable;
import java.util.ArrayList;
import java.util.List;
import org.apache.poi.ss.usermodel.Sheet;
import org.springframework.stereotype.Component;
import pl.chrono.reportmanager.dto.EmployeeOvertimeReport;
import pl.chrono.reportmanager.dto.OvertimeEntry;

@Component
public class EmployeeOvertimeReportGenerator
    extends AbstractReportGenerator<EmployeeOvertimeReport> {

  private static final String[] HEADERS = {"Data", "Przepracowano", "Wymagano", "Nadgodziny"};

  @Override
  protected void writeCsv(EmployeeOvertimeReport report, StringBuilder csv) {
    csvRow(csv, "Raport Nadgodzin (Od - Do):", period(report));
    csvRow(csv, "Pracownik:", report.employeeFullName());
    csvRow(csv, "Suma nadgodzin:", formatSeconds(report.totalOvertimeSeconds()));
    csvRow(csv);
    csvRow(csv, (Object[]) HEADERS);

    for (OvertimeEntry entry : report.entries()) {
      csvRow(csv, rowValues(entry));
    }
  }

  @Override
  protected void writePdf(EmployeeOvertimeReport report, Document document)
      throws DocumentException {
    addPdfTitle(document, title(report));
    for (String line : summaryLines(report)) {
      addPdfText(document, line);
    }
    addPdfSpacer(document);

    PdfPTable table = createPdfTable(HEADERS);
    for (OvertimeEntry entry : report.entries()) {
      addPdfRow(table, rowValues(entry));
    }
    document.add(table);
  }

  @Override
  protected String getSheetName() {
    return "Nadgodziny - Pracownik";
  }

  @Override
  protected void writeXlsx(EmployeeOvertimeReport report, Sheet sheet) {
    List<String> titleLines = new ArrayList<>();
    titleLines.add(title(report));
    titleLines.addAll(summaryLines(report));

    int rowIdx = writeXlsxHeader(sheet, titleLines, HEADERS);
    for (OvertimeEntry entry : report.entries()) {
      xlsxRow(sheet, rowIdx++, rowValues(entry));
    }
  }

  private String period(EmployeeOvertimeReport report) {
    return report.startDate() + " - " + report.endDate();
  }

  private String title(EmployeeOvertimeReport report) {
    return "Raport Nadgodzin: " + period(report);
  }

  private List<String> summaryLines(EmployeeOvertimeReport report) {
    return List.of(
        "Pracownik: " + report.employeeFullName(),
        "Suma nadgodzin: " + formatSeconds(report.totalOvertimeSeconds())
    );
  }

  private Object[] rowValues(OvertimeEntry entry) {
    return new Object[] {
        entry.date(),
        formatSeconds(entry.workedSeconds()),
        formatSeconds((long) entry.requiredSeconds()),
        formatSeconds(entry.overtimeSeconds())
    };
  }
}
