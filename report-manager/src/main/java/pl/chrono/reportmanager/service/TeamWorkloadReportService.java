package pl.chrono.reportmanager.service;

import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.chrono.dbmanager.model.User;
import pl.chrono.dbmanager.repository.UserRepository;
import pl.chrono.reportmanager.dto.EmployeeTimesheetReport;
import pl.chrono.reportmanager.dto.TeamWorkloadEntry;
import pl.chrono.reportmanager.dto.TeamWorkloadReport;

@Service
@RequiredArgsConstructor
public class TeamWorkloadReportService {

  private final UserRepository userRepository;
  private final TimesheetReportService timesheetReportService;

  @Transactional(readOnly = true)
  public TeamWorkloadReport generateReport(Integer managerId, LocalDate startDate, LocalDate endDate) {
    List<User> subordinates = userRepository.findByManagerUserId(managerId);

    List<TeamWorkloadEntry> entries = subordinates.stream().map(user -> {
      EmployeeTimesheetReport timesheet = timesheetReportService.generateTimesheet(user.getUserId(), startDate, endDate);

      return new TeamWorkloadEntry(
          user.getUserId(),
          user.getName() + " " + user.getSurname(),
          timesheet.totalWorkSeconds(),
          timesheet.totalRequiredSeconds(),
          timesheet.totalBalanceSeconds()
      );
    }).toList();

    return new TeamWorkloadReport(managerId, startDate, endDate, entries);
  }
}
