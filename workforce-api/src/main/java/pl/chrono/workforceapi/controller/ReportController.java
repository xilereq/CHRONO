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
import pl.chrono.reportmanager.dto.ReportFormat;
import pl.chrono.usermanager.service.AuthService;

@RestController
@RequestMapping("/api/reports")
public class ReportController extends BaseController {

  private final I_ReportFacade reportFacade;

  public ReportController(AuthService authService, I_ReportFacade reportFacade) {
    super(authService);
    this.reportFacade = reportFacade;
  }

  @GetMapping("/timesheet/me")
  @PreAuthorize("hasAnyRole('EMPLOYEE', 'MANAGER', 'HR', 'ADMIN')")
  public ResponseEntity<byte[]> getMyTimesheetReport(
      @RequestHeader("Authorization") String token,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
      @RequestParam(defaultValue = "CSV") ReportFormat format) {

    Integer employeeId = extractId(token);
    byte[] reportData = reportFacade.getEmployeeTimesheet(employeeId, startDate, endDate, format);

    String filename = format.fileName(String.format("my_timesheet_%s_to_%s", startDate, endDate));
    return createResponse(reportData, filename, format);
  }

  @GetMapping("/timesheet/employee/{employeeId}")
  @PreAuthorize("hasAnyRole('MANAGER', 'HR', 'ADMIN')")
  public ResponseEntity<byte[]> getEmployeeTimesheetReport(
      @PathVariable Integer employeeId,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
      @RequestParam(defaultValue = "CSV") ReportFormat format) {

    byte[] reportData = reportFacade.getEmployeeTimesheet(employeeId, startDate, endDate, format);

    String filename = format.fileName(String.format("employee_%d_timesheet_%s_to_%s", employeeId, startDate, endDate));
    return createResponse(reportData, filename, format);
  }

  @GetMapping("/overtime/me")
  @PreAuthorize("hasAnyRole('EMPLOYEE', 'MANAGER', 'HR', 'ADMIN')")
  public ResponseEntity<byte[]> getMyOvertimeReport(
      @RequestHeader("Authorization") String token,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
      @RequestParam(defaultValue = "CSV") ReportFormat format) {

    Integer employeeId = extractId(token);
    byte[] reportData = reportFacade.getEmployeeOvertime(employeeId, startDate, endDate, format);

    String filename = format.fileName(String.format("my_overtime_%s_to_%s", startDate, endDate));
    return createResponse(reportData, filename, format);
  }

  @GetMapping("/workload/team")
  @PreAuthorize("hasRole('MANAGER')")
  public ResponseEntity<byte[]> getTeamWorkloadReport(
      @RequestHeader("Authorization") String token,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
      @RequestParam(defaultValue = "CSV") ReportFormat format) {

    Integer managerId = extractId(token);
    byte[] reportData = reportFacade.getTeamWorkload(managerId, startDate, endDate, format);

    String filename = format.fileName(String.format("team_workload_%s_to_%s", startDate, endDate));
    return createResponse(reportData, filename, format);
  }

  @GetMapping("/absences/team")
  @PreAuthorize("hasRole('MANAGER')")
  public ResponseEntity<byte[]> getTeamAbsencesReport(
      @RequestHeader("Authorization") String token,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
      @RequestParam(defaultValue = "CSV") ReportFormat format) {

    Integer managerId = extractId(token);
    byte[] reportData = reportFacade.getTeamAbsences(managerId, startDate, endDate, format);

    String filename = format.fileName(String.format("team_absences_%s_to_%s", startDate, endDate));
    return createResponse(reportData, filename, format);
  }

  @GetMapping("/overtime/team")
  @PreAuthorize("hasRole('MANAGER')")
  public ResponseEntity<byte[]> getTeamOvertimeReport(
      @RequestHeader("Authorization") String token,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
      @RequestParam(defaultValue = "CSV") ReportFormat format) {

    Integer managerId = extractId(token);
    byte[] reportData = reportFacade.getTeamOvertime(managerId, startDate, endDate, format);

    String filename = format.fileName(String.format("team_overtime_%s_to_%s", startDate, endDate));
    return createResponse(reportData, filename, format);
  }

  @GetMapping("/anomalies/team")
  @PreAuthorize("hasRole('MANAGER')")
  public ResponseEntity<byte[]> getTeamAnomalyReport(
      @RequestHeader("Authorization") String token,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
      @RequestParam(defaultValue = "CSV") ReportFormat format) {

    Integer managerId = extractId(token);
    byte[] reportData = reportFacade.getTeamAnomalyReport(managerId, startDate, endDate, format);

    String filename = format.fileName(String.format("team_anomalies_%s_to_%s", startDate, endDate));
    return createResponse(reportData, filename, format);
  }

  private ResponseEntity<byte[]> createResponse(byte[] fileData, String filename, ReportFormat format) {
    return ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
        .contentType(MediaType.parseMediaType(format.getMediaType()))
        .body(fileData);
  }
}
