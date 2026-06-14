package pl.chrono.reportmanager.api;

import pl.chrono.reportmanager.dto.ReportFormat;
import java.time.LocalDate;

public interface I_ReportFacade {
  byte[] getEmployeeTimesheet(Integer employeeId, LocalDate startDate, LocalDate endDate, ReportFormat format);
  byte[] getTeamWorkload(Integer managerId, LocalDate startDate, LocalDate endDate, ReportFormat format);
  byte[] getTeamAbsences(Integer managerId, LocalDate startDate, LocalDate endDate, ReportFormat format);
  byte[] getEmployeeOvertime(Integer employeeId, LocalDate startDate, LocalDate endDate, ReportFormat format);
  byte[] getTeamOvertime(Integer managerId, LocalDate startDate, LocalDate endDate, ReportFormat format);
}
