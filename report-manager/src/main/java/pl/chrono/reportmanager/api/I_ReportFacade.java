package pl.chrono.reportmanager.api;

import java.time.LocalDate;

public interface I_ReportFacade {
  byte[] getEmployeeTimesheetCsv(Integer employeeId, LocalDate startDate, LocalDate endDate);
  byte[] getTeamWorkloadCsv(Integer managerId, LocalDate startDate, LocalDate endDate);
  byte[] getTeamAbsencesCsv(Integer managerId, LocalDate startDate, LocalDate endDate);
}
