package pl.chrono.reportmanager.generator;

import static pl.chrono.reportmanager.dto.ValueType.DATE;
import static pl.chrono.reportmanager.dto.ValueType.DURATION;
import static pl.chrono.reportmanager.dto.ValueType.TEXT;
import static pl.chrono.reportmanager.dto.ValueType.TIME;

import java.util.Arrays;
import java.util.List;
import org.springframework.stereotype.Component;
import pl.chrono.reportmanager.dto.DailyTimesheetEntry;
import pl.chrono.reportmanager.dto.EmployeeTimesheetReport;
import pl.chrono.reportmanager.dto.ReportColumn;
import pl.chrono.reportmanager.dto.ReportDocument;
import pl.chrono.reportmanager.dto.ReportField;

@Component
public class TimesheetReportGenerator implements I_ReportGenerator<EmployeeTimesheetReport> {

  private static final List<ReportColumn> COLUMNS = List.of(
      new ReportColumn("Data", DATE),
      new ReportColumn("Start", TIME),
      new ReportColumn("Koniec", TIME),
      new ReportColumn("Efektywny Czas", DURATION),
      new ReportColumn("Wymagany Czas", DURATION),
      new ReportColumn("Bilans", DURATION),
      new ReportColumn("Godziny Nocne", DURATION),
      new ReportColumn("Wniosek", TEXT)
  );

  @Override
  public ReportDocument generate(EmployeeTimesheetReport report) {
    List<ReportField> fields = List.of(
        ReportField.period(report.startDate(), report.endDate()),
        new ReportField("Pracownik", report.employeeFullName(), TEXT),
        new ReportField("Przepracowano", report.totalWorkSeconds(), DURATION),
        new ReportField("Wymagano", report.totalRequiredSeconds(), DURATION),
        new ReportField("Bilans", report.totalBalanceSeconds(), DURATION),
        new ReportField("Godziny Nocne", report.totalNightSeconds(), DURATION)
    );
    List<List<Object>> rows = report.dailyEntries().stream().map(this::toRow).toList();
    return new ReportDocument("Raport Okresowy", fields, COLUMNS, rows);
  }

  private List<Object> toRow(DailyTimesheetEntry entry) {
    return Arrays.asList(
        entry.date(),
        entry.startTime(),
        entry.endTime(),
        entry.effectiveWorkSeconds(),
        entry.requiredSeconds(),
        entry.balanceSeconds(),
        entry.nightSeconds(),
        entry.applicationType()
    );
  }
}
