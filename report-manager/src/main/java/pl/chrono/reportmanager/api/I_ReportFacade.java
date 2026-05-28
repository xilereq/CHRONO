package pl.chrono.reportmanager.api;

import java.time.LocalDate;

public interface I_ReportFacade {
  byte[] getEmployeeTimesheetCsv(Integer employeeId, int year, int month);
  byte[] getTeamWorkloadCsv(Integer managerId, int year, int month);
  byte[] getTeamAbsencesCsv(Integer managerId, LocalDate startDate, LocalDate endDate);
}
