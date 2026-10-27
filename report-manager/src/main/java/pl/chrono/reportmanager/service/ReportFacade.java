package pl.chrono.reportmanager.service;

import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import pl.chrono.reportmanager.api.I_ReportFacade;
import pl.chrono.reportmanager.dto.EmployeeOvertimeReport;
import pl.chrono.reportmanager.dto.EmployeeTimesheetReport;
import pl.chrono.reportmanager.dto.ReportFormat;
import pl.chrono.reportmanager.dto.TeamAbsencesReport;
import pl.chrono.reportmanager.dto.TeamAnomalyReport;
import pl.chrono.reportmanager.dto.TeamOvertimeReport;
import pl.chrono.reportmanager.dto.TeamWorkloadReport;
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
    EmployeeTimesheetReport reportData = timesheetService.generateTimesheet(employeeId, startDate, endDate);
    return exporter.export(timesheetGenerator.generate(reportData), format);
  }

  @Override
  public byte[] getTeamWorkload(Integer managerId, LocalDate startDate, LocalDate endDate, ReportFormat format) {
    TeamWorkloadReport reportData = workloadService.generateReport(managerId, startDate, endDate);
    return exporter.export(workloadGenerator.generate(reportData), format);
  }

  @Override
  public byte[] getTeamAbsences(Integer managerId, LocalDate startDate, LocalDate endDate, ReportFormat format) {
    TeamAbsencesReport reportData = absenceService.generateReport(managerId, startDate, endDate);
    return exporter.export(absenceGenerator.generate(reportData), format);
  }

  @Override
  public byte[] getEmployeeOvertime(Integer employeeId, LocalDate startDate, LocalDate endDate, ReportFormat format) {
    EmployeeOvertimeReport reportData = overtimeService.generateEmployeeReport(employeeId, startDate, endDate);
    return exporter.export(employeeOvertimeGenerator.generate(reportData), format);
  }

  @Override
  public byte[] getTeamOvertime(Integer managerId, LocalDate startDate, LocalDate endDate, ReportFormat format) {
    TeamOvertimeReport reportData = overtimeService.generateTeamReport(managerId, startDate, endDate);
    return exporter.export(teamOvertimeGenerator.generate(reportData), format);
  }

  @Override
  public byte[] getTeamAnomalyReport(Integer managerId, LocalDate startDate, LocalDate endDate, ReportFormat format) {
    TeamAnomalyReport reportData = anomalyService.generateReport(managerId, startDate, endDate);
    return exporter.export(anomalyGenerator.generate(reportData), format);
  }
}
