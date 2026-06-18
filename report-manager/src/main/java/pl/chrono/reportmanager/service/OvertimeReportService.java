package pl.chrono.reportmanager.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.chrono.dbmanager.model.User;
import pl.chrono.dbmanager.repository.UserRepository;
import pl.chrono.reportmanager.dto.*;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OvertimeReportService {

  private final TimesheetReportService timesheetReportService;
  private final UserRepository userRepository;

  @Transactional(readOnly = true)
  public EmployeeOvertimeReport generateEmployeeReport(Integer employeeId, LocalDate startDate, LocalDate endDate) {
    EmployeeTimesheetReport timesheet = timesheetReportService.generateTimesheet(employeeId, startDate, endDate);

    List<OvertimeEntry> overtimeEntries = timesheet.dailyEntries().stream()
        .filter(entry -> entry.balanceSeconds() > 0)
        .map(entry -> new OvertimeEntry(entry.date(), entry.effectiveWorkSeconds(), entry.requiredSeconds(), entry.balanceSeconds()))
        .toList();

    long totalOvertime = overtimeEntries.stream()
        .mapToLong(OvertimeEntry::overtimeSeconds)
        .sum();

    return new EmployeeOvertimeReport(employeeId, timesheet.employeeFullName(), startDate, endDate, totalOvertime, overtimeEntries);
  }

  @Transactional(readOnly = true)
  public TeamOvertimeReport generateTeamReport(Integer managerId, LocalDate startDate, LocalDate endDate) {
    List<User> subordinates = userRepository.findByManagerUserId(managerId);

    List<TeamOvertimeEntry> entries = subordinates.stream()
        .map(user -> {
          EmployeeOvertimeReport empReport = generateEmployeeReport(user.getUserId(), startDate, endDate);
          return new TeamOvertimeEntry(user.getUserId(), empReport.employeeFullName(), empReport.totalOvertimeSeconds());
        })
        .filter(entry -> entry.totalOvertimeSeconds() > 0)
        .toList();

    return new TeamOvertimeReport(managerId, startDate, endDate, entries);
  }
}
