package pl.chrono.reportmanager.generator;

import static pl.chrono.reportmanager.dto.ValueType.DATE;
import static pl.chrono.reportmanager.dto.ValueType.TEXT;

import java.util.Arrays;
import java.util.List;
import org.springframework.stereotype.Component;
import pl.chrono.reportmanager.dto.ReportColumn;
import pl.chrono.reportmanager.dto.ReportDocument;
import pl.chrono.reportmanager.dto.ReportField;
import pl.chrono.reportmanager.dto.TeamAbsenceEntry;
import pl.chrono.reportmanager.dto.TeamAbsencesReport;

@Component
public class AbsenceReportGenerator implements I_ReportGenerator<TeamAbsencesReport> {

  private static final List<ReportColumn> COLUMNS = List.of(
      new ReportColumn("Pracownik", TEXT),
      new ReportColumn("Typ Wniosku", TEXT),
      new ReportColumn("Status", TEXT),
      new ReportColumn("Data Od", DATE),
      new ReportColumn("Data Do", DATE)
  );

  @Override
  public ReportDocument generate(TeamAbsencesReport report) {
    List<ReportField> fields = List.of(ReportField.period(report.startDate(), report.endDate()));
    List<List<Object>> rows = report.entries().stream().map(this::toRow).toList();
    return new ReportDocument("Raport Wnioskow i Absencji", fields, COLUMNS, rows);
  }

  private List<Object> toRow(TeamAbsenceEntry entry) {
    return Arrays.asList(
        entry.employeeFullName(),
        entry.applicationType(),
        entry.status(),
        entry.startDate(),
        entry.endDate()
    );
  }
}
