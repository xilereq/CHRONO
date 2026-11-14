package pl.chrono.reportmanager.generator;

import static pl.chrono.reportmanager.dto.ValueType.DATE;
import static pl.chrono.reportmanager.dto.ValueType.DURATION;

import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;
import pl.chrono.reportmanager.dto.ReportColumn;
import pl.chrono.reportmanager.dto.ReportDocument;
import pl.chrono.reportmanager.dto.ReportField;
import pl.chrono.reportmanager.dto.TeamMatrixDay;
import pl.chrono.reportmanager.dto.TeamMatrixReport;

@Component
public class TeamMatrixReportGenerator implements I_ReportGenerator<TeamMatrixReport> {

  @Override
  public ReportDocument generate(TeamMatrixReport report) {
    List<ReportField> fields = List.of(ReportField.period(report.startDate(), report.endDate()));
    List<List<Object>> rows = report.days().stream().map(this::toRow).toList();
    return new ReportDocument("Miesięczna Macierz Zespołu", fields, columns(report), rows);
  }

  private List<ReportColumn> columns(TeamMatrixReport report) {
    List<ReportColumn> columns = new ArrayList<>();
    columns.add(new ReportColumn("Data", DATE));
    report.employeeNames().forEach(name -> columns.add(new ReportColumn(name, DURATION)));
    return columns;
  }

  private List<Object> toRow(TeamMatrixDay day) {
    List<Object> row = new ArrayList<>();
    row.add(day.date());
    row.addAll(day.workSeconds());
    return row;
  }
}
