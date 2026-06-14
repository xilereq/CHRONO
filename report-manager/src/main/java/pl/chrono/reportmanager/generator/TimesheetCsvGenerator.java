package pl.chrono.reportmanager.generator;

import org.springframework.stereotype.Component;
import pl.chrono.reportmanager.dto.EmployeeTimesheetReport;
import pl.chrono.reportmanager.dto.DailyTimesheetEntry;
import java.nio.charset.StandardCharsets;

@Component
public class TimesheetCsvGenerator implements I_CsvGenerator<EmployeeTimesheetReport> {
  @Override
  public byte[] generateCsv(EmployeeTimesheetReport report) {
    StringBuilder sb = new StringBuilder();
    sb.append("Raport Okresowy:;").append(report.startDate()).append(" do ").append(report.endDate()).append("\n");
    sb.append("Pracownik:;").append(report.employeeFullName()).append("\n");
    sb.append("Przepracowano:;").append(formatSeconds(report.totalWorkSeconds())).append("\n");
    sb.append("Wymagano:;").append(formatSeconds(report.totalRequiredSeconds())).append("\n");
    sb.append("Bilans:;").append(formatSeconds(report.totalBalanceSeconds())).append("\n\n");

    sb.append("Data;Start;Koniec;Efektywny Czas;Wymagany Czas;Bilans;Wniosek\n");

    for (DailyTimesheetEntry entry : report.dailyEntries()) {
      sb.append(entry.date()).append(";");
      sb.append(entry.startTime() != null ? entry.startTime().toString() : "-").append(";");
      sb.append(entry.endTime() != null ? entry.endTime().toString() : "-").append(";");
      sb.append(formatSeconds(entry.effectiveWorkSeconds())).append(";");
      sb.append(formatSeconds((long) entry.requiredSeconds())).append(";");
      sb.append(formatSeconds(entry.balanceSeconds())).append(";");
      sb.append(entry.applicationType() != null ? entry.applicationType() : "").append("\n");
    }

    return addUtf8Bom(sb.toString());
  }

  private byte[] addUtf8Bom(String content) {
    byte[] bytes = content.getBytes(StandardCharsets.UTF_8);
    byte[] withBom = new byte[bytes.length + 3];
    withBom[0] = (byte) 0xEF;
    withBom[1] = (byte) 0xBB;
    withBom[2] = (byte) 0xBF;
    System.arraycopy(bytes, 0, withBom, 3, bytes.length);
    return withBom;
  }
}
