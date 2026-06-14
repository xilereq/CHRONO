package pl.chrono.reportmanager.service;

import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import pl.chrono.reportmanager.api.I_ReportFacade;
import pl.chrono.reportmanager.dto.EmployeeTimesheetReport;
import pl.chrono.reportmanager.dto.TeamAbsencesReport;
import pl.chrono.reportmanager.dto.TeamWorkloadReport;
import pl.chrono.reportmanager.generator.AbsenceCsvGenerator;
import pl.chrono.reportmanager.generator.TimesheetCsvGenerator;
import pl.chrono.reportmanager.generator.WorkloadCsvGenerator;

@Service
@RequiredArgsConstructor
public class ReportFacade implements I_ReportFacade {

  private final TimesheetReportService timesheetService;
  private final TeamWorkloadReportService workloadService;
  private final TeamAbsenceReportService absenceService;

  private final TimesheetCsvGenerator timesheetGenerator;
  private final WorkloadCsvGenerator workloadGenerator;
  private final AbsenceCsvGenerator absenceGenerator;

  @Override
  public byte[] getEmployeeTimesheetCsv(Integer employeeId, LocalDate startDate, LocalDate endDate) {
    EmployeeTimesheetReport reportData = timesheetService.generateTimesheet(employeeId, startDate, endDate);
    return timesheetGenerator.generateCsv(reportData);
  }

  @Override
  public byte[] getTeamWorkloadCsv(Integer managerId, LocalDate startDate, LocalDate endDate) {
    TeamWorkloadReport reportData = workloadService.generateReport(managerId, startDate, endDate);
    return workloadGenerator.generateCsv(reportData);
  }
  @Override
  public byte[] getTeamAbsencesCsv(Integer managerId, LocalDate startDate, LocalDate endDate) {
    TeamAbsencesReport reportData = absenceService.generateReport(managerId, startDate, endDate);
    return absenceGenerator.generateCsv(reportData);
  }
}
