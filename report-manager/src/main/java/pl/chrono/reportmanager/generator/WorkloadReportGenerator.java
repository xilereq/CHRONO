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
import pl.chrono.reportmanager.dto.TeamWorkloadEntry;
import pl.chrono.reportmanager.dto.TeamWorkloadReport;

@Component
public class WorkloadReportGenerator implements I_ReportGenerator<TeamWorkloadReport> {

  private static final List<ReportColumn> COLUMNS = List.of(
      new ReportColumn("ID Pracownika", NUMBER),
      new ReportColumn("Imie i Nazwisko", TEXT),
      new ReportColumn("Przepracowano", DURATION),
      new ReportColumn("Wymagano", DURATION),
      new ReportColumn("Bilans", DURATION)
  );

  @Override
  public ReportDocument generate(TeamWorkloadReport report) {
    List<ReportField> fields = List.of(ReportField.period(report.startDate(), report.endDate()));
    List<List<Object>> rows = report.entries().stream().map(this::toRow).toList();
    return new ReportDocument("Raport Obciazenia Zespolu", fields, COLUMNS, rows);
  }

  private List<Object> toRow(TeamWorkloadEntry entry) {
    return Arrays.asList(
        entry.employeeId(),
        entry.employeeFullName(),
        entry.totalWorkSeconds(),
        entry.totalRequiredSeconds(),
        entry.totalBalanceSeconds()
    );
  }
}
