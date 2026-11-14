package pl.chrono.reportmanager.service;

import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import pl.chrono.reportmanager.api.I_ReportFacade;
import pl.chrono.reportmanager.dto.ReportDocument;
import pl.chrono.reportmanager.dto.ReportFormat;
import pl.chrono.reportmanager.generator.AbsenceReportGenerator;
import pl.chrono.reportmanager.generator.AnomalyReportGenerator;
import pl.chrono.reportmanager.generator.EmployeeOvertimeReportGenerator;
import pl.chrono.reportmanager.generator.TeamOvertimeReportGenerator;
import pl.chrono.reportmanager.generator.TimesheetReportGenerator;
import pl.chrono.reportmanager.generator.WorkloadReportGenerator;
import pl.chrono.reportmanager.renderer.ReportExporter;

@Service
@RequiredArgsConstructor
public class ReportFacade implements I_ReportFacade {

  private final TimesheetReportService timesheetService;
  private final TeamWorkloadReportService workloadService;
  private final TeamAbsenceReportService absenceService;
  private final OvertimeReportService overtimeService;
  private final TeamAnomalyReportService anomalyService;

  private final TimesheetReportGenerator timesheetGenerator;
  private final WorkloadReportGenerator workloadGenerator;
  private final AbsenceReportGenerator absenceGenerator;
  private final EmployeeOvertimeReportGenerator employeeOvertimeGenerator;
  private final TeamOvertimeReportGenerator teamOvertimeGenerator;
  private final AnomalyReportGenerator anomalyGenerator;

  private final ReportExporter exporter;

  @Override
  public byte[] getEmployeeTimesheet(Integer employeeId, LocalDate startDate, LocalDate endDate, ReportFormat format) {
    return exporter.export(getEmployeeTimesheetDocument(employeeId, startDate, endDate), format);
  }

  @Override
  public byte[] getTeamWorkload(Integer managerId, LocalDate startDate, LocalDate endDate, ReportFormat format) {
    return exporter.export(getTeamWorkloadDocument(managerId, startDate, endDate), format);
  }

  @Override
  public byte[] getTeamAbsences(Integer managerId, LocalDate startDate, LocalDate endDate, ReportFormat format) {
    return exporter.export(getTeamAbsencesDocument(managerId, startDate, endDate), format);
  }

  @Override
  public byte[] getEmployeeOvertime(Integer employeeId, LocalDate startDate, LocalDate endDate, ReportFormat format) {
    return exporter.export(getEmployeeOvertimeDocument(employeeId, startDate, endDate), format);
  }

  @Override
  public byte[] getTeamOvertime(Integer managerId, LocalDate startDate, LocalDate endDate, ReportFormat format) {
    return exporter.export(getTeamOvertimeDocument(managerId, startDate, endDate), format);
  }

  @Override
  public byte[] getTeamAnomalyReport(Integer managerId, LocalDate startDate, LocalDate endDate, ReportFormat format) {
    return exporter.export(getTeamAnomalyDocument(managerId, startDate, endDate), format);
  }

  @Override
  public ReportDocument getEmployeeTimesheetDocument(Integer employeeId, LocalDate startDate, LocalDate endDate) {
    return timesheetGenerator.generate(timesheetService.generateTimesheet(employeeId, startDate, endDate));
  }

  @Override
  public ReportDocument getTeamWorkloadDocument(Integer managerId, LocalDate startDate, LocalDate endDate) {
    return workloadGenerator.generate(workloadService.generateReport(managerId, startDate, endDate));
  }

  @Override
  public ReportDocument getTeamAbsencesDocument(Integer managerId, LocalDate startDate, LocalDate endDate) {
    return absenceGenerator.generate(absenceService.generateReport(managerId, startDate, endDate));
  }

  @Override
  public ReportDocument getEmployeeOvertimeDocument(Integer employeeId, LocalDate startDate, LocalDate endDate) {
    return employeeOvertimeGenerator.generate(
        overtimeService.generateEmployeeReport(employeeId, startDate, endDate));
  }

  @Override
  public ReportDocument getTeamOvertimeDocument(Integer managerId, LocalDate startDate, LocalDate endDate) {
    return teamOvertimeGenerator.generate(overtimeService.generateTeamReport(managerId, startDate, endDate));
  }

  @Override
  public ReportDocument getTeamAnomalyDocument(Integer managerId, LocalDate startDate, LocalDate endDate) {
    return anomalyGenerator.generate(anomalyService.generateReport(managerId, startDate, endDate));
  }
}
