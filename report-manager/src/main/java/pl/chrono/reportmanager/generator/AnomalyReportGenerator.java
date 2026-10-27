package pl.chrono.reportmanager.generator;

import static pl.chrono.reportmanager.dto.ValueType.DURATION;
import static pl.chrono.reportmanager.dto.ValueType.TEXT;

import java.util.Arrays;
import java.util.List;
import org.springframework.stereotype.Component;
import pl.chrono.reportmanager.dto.ReportColumn;
import pl.chrono.reportmanager.dto.ReportDocument;
import pl.chrono.reportmanager.dto.ReportField;
import pl.chrono.reportmanager.dto.TeamAnomalyEntry;
import pl.chrono.reportmanager.dto.TeamAnomalyReport;

@Component
public class AnomalyReportGenerator implements I_ReportGenerator<TeamAnomalyReport> {

  private static final List<ReportColumn> COLUMNS = List.of(
      new ReportColumn("Pracownik", TEXT),
      new ReportColumn("Przepracowano", DURATION),
      new ReportColumn("Wymagano", DURATION),
      new ReportColumn("Bilans", DURATION),
      new ReportColumn("Typ Anomalii", TEXT)
  );

  @Override
  public ReportDocument generate(TeamAnomalyReport report) {
    List<ReportField> fields = List.of(ReportField.period(report.startDate(), report.endDate()));
    List<List<Object>> rows = report.entries().stream().map(this::toRow).toList();
    return new ReportDocument("Raport Anomalii Czasu Pracy", fields, COLUMNS, rows);
  }

  private List<Object> toRow(TeamAnomalyEntry entry) {
    return Arrays.asList(
        entry.employeeFullName(),
        entry.totalWorkSeconds(),
        entry.totalRequiredSeconds(),
        entry.balanceSeconds(),
        entry.anomalyType()
    );
  }
}
