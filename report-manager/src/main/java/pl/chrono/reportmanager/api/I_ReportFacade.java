package pl.chrono.reportmanager.api;

import java.time.LocalDate;
import pl.chrono.reportmanager.dto.ReportDocument;
import pl.chrono.reportmanager.dto.ReportFormat;

public interface I_ReportFacade {

  byte[] getEmployeeTimesheet(Integer employeeId, LocalDate startDate, LocalDate endDate, ReportFormat format);

  byte[] getTeamWorkload(Integer managerId, LocalDate startDate, LocalDate endDate, ReportFormat format);

  byte[] getTeamAbsences(Integer managerId, LocalDate startDate, LocalDate endDate, ReportFormat format);

  byte[] getEmployeeOvertime(Integer employeeId, LocalDate startDate, LocalDate endDate, ReportFormat format);

  byte[] getTeamOvertime(Integer managerId, LocalDate startDate, LocalDate endDate, ReportFormat format);

  byte[] getTeamAnomalyReport(Integer managerId, LocalDate startDate, LocalDate endDate, ReportFormat format);

  ReportDocument getEmployeeTimesheetDocument(Integer employeeId, LocalDate startDate, LocalDate endDate);

  ReportDocument getTeamWorkloadDocument(Integer managerId, LocalDate startDate, LocalDate endDate);

  ReportDocument getTeamAbsencesDocument(Integer managerId, LocalDate startDate, LocalDate endDate);

  ReportDocument getEmployeeOvertimeDocument(Integer employeeId, LocalDate startDate, LocalDate endDate);

  ReportDocument getTeamOvertimeDocument(Integer managerId, LocalDate startDate, LocalDate endDate);

  ReportDocument getTeamAnomalyDocument(Integer managerId, LocalDate startDate, LocalDate endDate);
}
