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
import pl.chrono.reportmanager.dto.TeamNightHoursEntry;
import pl.chrono.reportmanager.dto.TeamNightHoursReport;

@Component
public class TeamNightHoursReportGenerator implements I_ReportGenerator<TeamNightHoursReport> {

  private static final List<ReportColumn> COLUMNS = List.of(
      new ReportColumn("ID Pracownika", NUMBER),
      new ReportColumn("Imię i Nazwisko", TEXT),
      new ReportColumn("Dni z pracą w nocy", NUMBER),
      new ReportColumn("Godziny nocne", DURATION)
  );

  @Override
  public ReportDocument generate(TeamNightHoursReport report) {
    List<ReportField> fields = List.of(ReportField.period(report.startDate(), report.endDate()));
    List<List<Object>> rows = report.entries().stream().map(this::toRow).toList();
    return new ReportDocument("Raport Godzin Nocnych Zespołu", fields, COLUMNS, rows);
  }

  private List<Object> toRow(TeamNightHoursEntry entry) {
    return Arrays.asList(
        entry.employeeId(),
        entry.employeeFullName(),
        entry.nightShifts(),
        entry.nightSeconds()
    );
  }
}
