package pl.chrono.reportmanager.generator;

import static pl.chrono.reportmanager.dto.ValueType.DURATION;
import static pl.chrono.reportmanager.dto.ValueType.NUMBER;
import static pl.chrono.reportmanager.dto.ValueType.TEXT;

import java.util.Arrays;
import java.util.List;
import org.springframework.stereotype.Component;
import pl.chrono.reportmanager.dto.ReportColumn;
import pl.chrono.reportmanager.dto.ReportDocument;
import pl.chrono.reportmanager.dto.ReportField;
import pl.chrono.reportmanager.dto.TeamOvertimeEntry;
import pl.chrono.reportmanager.dto.TeamOvertimeReport;

@Component
public class TeamOvertimeReportGenerator implements I_ReportGenerator<TeamOvertimeReport> {

  private static final List<ReportColumn> COLUMNS = List.of(
      new ReportColumn("ID Pracownika", NUMBER),
      new ReportColumn("Imie i Nazwisko", TEXT),
      new ReportColumn("Suma Nadgodzin", DURATION)
  );

  @Override
  public ReportDocument generate(TeamOvertimeReport report) {
    List<ReportField> fields = List.of(ReportField.period(report.startDate(), report.endDate()));
    List<List<Object>> rows = report.entries().stream().map(this::toRow).toList();
    return new ReportDocument("Zbiorczy Raport Nadgodzin Zespolu", fields, COLUMNS, rows);
  }

  private List<Object> toRow(TeamOvertimeEntry entry) {
    return Arrays.asList(
        entry.employeeId(),
        entry.employeeFullName(),
        entry.totalOvertimeSeconds()
    );
  }
}
