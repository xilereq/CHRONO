package pl.chrono.reportmanager.service;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.IntStream;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.chrono.dbmanager.repository.UserRepository;
import pl.chrono.reportmanager.dto.DailyTimesheetEntry;
import pl.chrono.reportmanager.dto.EmployeeTimesheetReport;
import pl.chrono.reportmanager.dto.TeamDailyOvertimeEntry;
import pl.chrono.reportmanager.dto.TeamDailyOvertimeReport;
import pl.chrono.reportmanager.dto.TeamMatrixDay;
import pl.chrono.reportmanager.dto.TeamMatrixReport;
import pl.chrono.reportmanager.dto.TeamNightHoursEntry;
import pl.chrono.reportmanager.dto.TeamNightHoursReport;

@Service
@RequiredArgsConstructor
public class TeamOperationalReportService {

  private final UserRepository userRepository;
  private final TimesheetReportService timesheetReportService;

  @Transactional(readOnly = true)
  public TeamMatrixReport generateMatrix(Integer managerId, LocalDate startDate, LocalDate endDate) {
    List<EmployeeTimesheetReport> timesheets = teamTimesheets(managerId, startDate, endDate);
    List<String> employeeNames = timesheets.stream()
        .map(EmployeeTimesheetReport::employeeFullName)
        .toList();
    List<TeamMatrixDay> days = IntStream.range(0, dayCount(startDate, endDate))
        .mapToObj(dayIndex -> matrixDay(startDate.plusDays(dayIndex), dayIndex, timesheets))
        .toList();
    return new TeamMatrixReport(managerId, startDate, endDate, employeeNames, days);
  }

  @Transactional(readOnly = true)
  public TeamDailyOvertimeReport generateDailyOvertime(Integer managerId, LocalDate startDate, LocalDate endDate) {
    List<EmployeeTimesheetReport> timesheets = teamTimesheets(managerId, startDate, endDate);
    List<TeamDailyOvertimeEntry> entries = IntStream.range(0, dayCount(startDate, endDate))
        .mapToObj(dayIndex -> dailyOvertime(startDate.plusDays(dayIndex), dayIndex, timesheets))
        .toList();
    return new TeamDailyOvertimeReport(managerId, startDate, endDate, entries);
  }

  @Transactional(readOnly = true)
  public TeamNightHoursReport generateNightHours(Integer managerId, LocalDate startDate, LocalDate endDate) {
    List<TeamNightHoursEntry> entries = teamTimesheets(managerId, startDate, endDate).stream()
        .filter(timesheet -> timesheet.totalNightSeconds() > 0)
        .map(this::nightHours)
        .toList();
    return new TeamNightHoursReport(managerId, startDate, endDate, entries);
  }

  private List<EmployeeTimesheetReport> teamTimesheets(Integer managerId, LocalDate startDate, LocalDate endDate) {
    return userRepository.findByManagerUserId(managerId).stream()
        .map(user -> timesheetReportService.generateTimesheet(user.getUserId(), startDate, endDate))
        .toList();
  }

  private TeamMatrixDay matrixDay(LocalDate date, int dayIndex, List<EmployeeTimesheetReport> timesheets) {
    List<Long> workSeconds = timesheets.stream()
        .map(timesheet -> timesheet.dailyEntries().get(dayIndex).effectiveWorkSeconds())
        .toList();
    return new TeamMatrixDay(date, workSeconds);
  }

  private TeamDailyOvertimeEntry dailyOvertime(LocalDate date, int dayIndex,
                                               List<EmployeeTimesheetReport> timesheets) {
    List<Long> overtimes = timesheets.stream()
        .map(timesheet -> timesheet.dailyEntries().get(dayIndex).balanceSeconds())
        .filter(balance -> balance > 0)
        .toList();
    long total = overtimes.stream().mapToLong(Long::longValue).sum();
    return new TeamDailyOvertimeEntry(date, overtimes.size(), total);
  }

  private TeamNightHoursEntry nightHours(EmployeeTimesheetReport timesheet) {
    int nightShifts = (int) timesheet.dailyEntries().stream()
        .map(DailyTimesheetEntry::nightSeconds)
        .filter(nightSeconds -> nightSeconds > 0)
        .count();
    return new TeamNightHoursEntry(timesheet.employeeId(), timesheet.employeeFullName(),
        nightShifts, timesheet.totalNightSeconds());
  }

  private int dayCount(LocalDate startDate, LocalDate endDate) {
    return (int) startDate.datesUntil(endDate.plusDays(1)).count();
  }
}
