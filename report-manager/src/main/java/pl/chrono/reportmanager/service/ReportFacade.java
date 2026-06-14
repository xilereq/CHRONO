package pl.chrono.reportmanager.service;

import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import pl.chrono.reportmanager.api.I_ReportFacade;
import pl.chrono.reportmanager.dto.EmployeeTimesheetReport;
import pl.chrono.reportmanager.dto.ReportFormat;
import pl.chrono.reportmanager.dto.TeamAbsencesReport;
import pl.chrono.reportmanager.dto.TeamWorkloadReport;
import pl.chrono.reportmanager.generator.AbsenceReportGenerator;
import pl.chrono.reportmanager.generator.TimesheetReportGenerator;
import pl.chrono.reportmanager.generator.WorkloadReportGenerator;

@Service
@RequiredArgsConstructor
public class ReportFacade implements I_ReportFacade {

  private final TimesheetReportService timesheetService;
  private final TeamWorkloadReportService workloadService;
  private final TeamAbsenceReportService absenceService;

  private final TimesheetReportGenerator timesheetGenerator;
  private final WorkloadReportGenerator workloadGenerator;
  private final AbsenceReportGenerator absenceGenerator;

  @Override
  public byte[] getEmployeeTimesheet(Integer employeeId, LocalDate startDate, LocalDate endDate, ReportFormat format) {
    EmployeeTimesheetReport reportData = timesheetService.generateTimesheet(employeeId, startDate, endDate);
    return timesheetGenerator.generate(reportData, format);
  }

  @Override
  public byte[] getTeamWorkload(Integer managerId, LocalDate startDate, LocalDate endDate, ReportFormat format) {
    TeamWorkloadReport reportData = workloadService.generateReport(managerId, startDate, endDate);
    return workloadGenerator.generate(reportData, format);
  }

  @Override
  public byte[] getTeamAbsences(Integer managerId, LocalDate startDate, LocalDate endDate, ReportFormat format) {
    TeamAbsencesReport reportData = absenceService.generateReport(managerId, startDate, endDate);
    return absenceGenerator.generate(reportData, format);
  }
}
