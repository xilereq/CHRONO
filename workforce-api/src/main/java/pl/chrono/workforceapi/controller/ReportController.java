package pl.chrono.workforceapi.controller;

import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pl.chrono.reportmanager.api.I_ReportFacade;
import pl.chrono.usermanager.service.AuthService;

@RestController
@RequestMapping("/api/reports")
public class ReportController extends BaseController {

  private final I_ReportFacade reportFacade;

  public ReportController(AuthService authService, I_ReportFacade reportFacade) {
    super(authService);
    this.reportFacade = reportFacade;
  }

  @GetMapping("/timesheet/my")
  @PreAuthorize("hasAnyRole('EMPLOYEE', 'MANAGER', 'HR', 'ADMIN')")
  public ResponseEntity<byte[]> getMyTimesheetReport(
      @RequestHeader("Authorization") String token,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

    Integer employeeId = extractId(token);
    byte[] csvData = reportFacade.getEmployeeTimesheetCsv(employeeId, startDate, endDate);

    String filename = String.format("my_timesheet_%s_to_%s.csv", startDate.toString(), endDate.toString());
    return createCsvResponse(csvData, filename);
  }

  @GetMapping("/timesheet/employee/{employeeId}")
  @PreAuthorize("hasAnyRole('MANAGER', 'HR', 'ADMIN')")
  public ResponseEntity<byte[]> getEmployeeTimesheetReport(
      @PathVariable Integer employeeId,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

    byte[] csvData = reportFacade.getEmployeeTimesheetCsv(employeeId, startDate, endDate);

    String filename = String.format("employee_%d_timesheet_%s_to_%s.csv", employeeId, startDate.toString(), endDate.toString());
    return createCsvResponse(csvData, filename);
  }

  @GetMapping("/team/workload")
  @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
  public ResponseEntity<byte[]> getTeamWorkloadReport(
      @RequestHeader("Authorization") String token,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

    Integer managerId = extractId(token);
    byte[] csvData = reportFacade.getTeamWorkloadCsv(managerId, startDate, endDate);

    String filename = String.format("team_workload_%s_to_%s.csv", startDate.toString(), endDate.toString());
    return createCsvResponse(csvData, filename);
  }

  @GetMapping("/team/absences")
  @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
  public ResponseEntity<byte[]> getTeamAbsencesReport(
      @RequestHeader("Authorization") String token,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

    Integer managerId = extractId(token);
    byte[] csvData = reportFacade.getTeamAbsencesCsv(managerId, startDate, endDate);

    String filename = String.format("team_absences_%s_to_%s.csv", startDate.toString(), endDate.toString());
    return createCsvResponse(csvData, filename);
  }

  private ResponseEntity<byte[]> createCsvResponse(byte[] csvData, String filename) {
    return ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
        .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
        .body(csvData);
  }
}
