package pl.chrono.reportmanager.generator;

import static pl.chrono.reportmanager.dto.ValueType.DATE;
import static pl.chrono.reportmanager.dto.ValueType.DURATION;
import static pl.chrono.reportmanager.dto.ValueType.TEXT;

import java.util.Arrays;
import java.util.List;
import org.springframework.stereotype.Component;
import pl.chrono.reportmanager.dto.EmployeeOvertimeReport;
import pl.chrono.reportmanager.dto.OvertimeEntry;
import pl.chrono.reportmanager.dto.ReportColumn;
import pl.chrono.reportmanager.dto.ReportDocument;
import pl.chrono.reportmanager.dto.ReportField;

@Component
public class EmployeeOvertimeReportGenerator implements I_ReportGenerator<EmployeeOvertimeReport> {

  private static final List<ReportColumn> COLUMNS = List.of(
      new ReportColumn("Data", DATE),
      new ReportColumn("Przepracowano", DURATION),
      new ReportColumn("Wymagano", DURATION),
      new ReportColumn("Nadgodziny", DURATION)
  );

  @Override
  public ReportDocument generate(EmployeeOvertimeReport report) {
    List<ReportField> fields = List.of(
        ReportField.period(report.startDate(), report.endDate()),
        new ReportField("Pracownik", report.employeeFullName(), TEXT),
        new ReportField("Suma nadgodzin", report.totalOvertimeSeconds(), DURATION)
    );
    List<List<Object>> rows = report.entries().stream().map(this::toRow).toList();
    return new ReportDocument("Raport Nadgodzin", fields, COLUMNS, rows);
  }

  private List<Object> toRow(OvertimeEntry entry) {
    return Arrays.asList(
        entry.date(),
        entry.workedSeconds(),
        entry.requiredSeconds(),
        entry.overtimeSeconds()
    );
  }
}
