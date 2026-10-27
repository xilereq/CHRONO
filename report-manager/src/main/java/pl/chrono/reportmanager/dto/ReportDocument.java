package pl.chrono.reportmanager.dto;

import java.util.List;
import java.util.stream.IntStream;

public record ReportDocument(
    String title,
    List<ReportField> fields,
    List<ReportColumn> columns,
    List<List<Object>> rows) {

  public List<String> headers() {
    return columns.stream().map(ReportColumn::label).toList();
  }

  public List<String> formatRow(List<Object> row) {
    return IntStream.range(0, columns.size())
        .mapToObj(i -> columns.get(i).type().format(row.get(i)))
        .toList();
  }
}
