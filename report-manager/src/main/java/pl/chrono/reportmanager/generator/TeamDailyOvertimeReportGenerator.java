package pl.chrono.reportmanager.generator;

import static pl.chrono.reportmanager.dto.ValueType.DATE;
import static pl.chrono.reportmanager.dto.ValueType.DURATION;
import static pl.chrono.reportmanager.dto.ValueType.NUMBER;

import java.util.Arrays;
import java.util.List;
import org.springframework.stereotype.Component;
import pl.chrono.reportmanager.dto.ReportColumn;
import pl.chrono.reportmanager.dto.ReportDocument;
import pl.chrono.reportmanager.dto.ReportField;
import pl.chrono.reportmanager.dto.TeamDailyOvertimeEntry;
import pl.chrono.reportmanager.dto.TeamDailyOvertimeReport;

@Component
public class TeamDailyOvertimeReportGenerator implements I_ReportGenerator<TeamDailyOvertimeReport> {

  private static final List<ReportColumn> COLUMNS = List.of(
      new ReportColumn("Data", DATE),
      new ReportColumn("Osoby z nadgodzinami", NUMBER),
      new ReportColumn("Suma nadgodzin", DURATION)
  );

  @Override
  public ReportDocument generate(TeamDailyOvertimeReport report) {
    long totalOvertime = report.entries().stream()
        .mapToLong(TeamDailyOvertimeEntry::totalOvertimeSeconds)
        .sum();
    List<ReportField> fields = List.of(
        ReportField.period(report.startDate(), report.endDate()),
        new ReportField("Suma nadgodzin", totalOvertime, DURATION)
    );
    List<List<Object>> rows = report.entries().stream().map(this::toRow).toList();
    return new ReportDocument("Nadgodziny Zespołu wg Dnia", fields, COLUMNS, rows);
  }

  private List<Object> toRow(TeamDailyOvertimeEntry entry) {
    return Arrays.asList(entry.date(), entry.employeesWithOvertime(), entry.totalOvertimeSeconds());
  }
}
