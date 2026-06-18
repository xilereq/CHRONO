package pl.chrono.reportmanager.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.chrono.dbmanager.model.User;
import pl.chrono.dbmanager.repository.UserRepository;
import pl.chrono.reportmanager.dto.EmployeeTimesheetReport;
import pl.chrono.reportmanager.dto.TeamAnomalyEntry;
import pl.chrono.reportmanager.dto.TeamAnomalyReport;

@Service
@RequiredArgsConstructor
public class TeamAnomalyReportService {

  private final UserRepository userRepository;
  private final TimesheetReportService timesheetReportService;

  @Transactional(readOnly = true)
  public TeamAnomalyReport generateReport(Integer managerId, LocalDate startDate, LocalDate endDate) {
    List<User> subordinates = userRepository.findByManagerUserId(managerId);
    List<TeamAnomalyEntry> anomalies = new ArrayList<>();

    for (User user : subordinates) {
      EmployeeTimesheetReport timesheet = timesheetReportService.generateTimesheet(user.getUserId(), startDate, endDate);

      long balance = timesheet.totalBalanceSeconds();
      long required = timesheet.totalRequiredSeconds();

      long threshold = (long) (required * 0.10);

      if (Math.abs(balance) > threshold) {
        String type = balance > 0 ? "ZNACZNE NADGODZINY" : "POWAZNY NIEDOBOR";

        anomalies.add(new TeamAnomalyEntry(
            user.getUserId(),
            user.getName() + " " + user.getSurname(),
            timesheet.totalWorkSeconds(),
            required,
            balance,
            type
        ));
      }
    }

    return new TeamAnomalyReport(managerId, startDate, endDate, anomalies);
  }
}
