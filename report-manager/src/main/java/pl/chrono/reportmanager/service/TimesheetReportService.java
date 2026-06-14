package pl.chrono.reportmanager.service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.chrono.dbmanager.model.Application;
import pl.chrono.dbmanager.model.User;
import pl.chrono.dbmanager.model.WorkLog;
import pl.chrono.dbmanager.repository.ApplicationRepository;
import pl.chrono.dbmanager.repository.UserRepository;
import pl.chrono.dbmanager.repository.WorkLogRepository;
import pl.chrono.reportmanager.dto.DailyTimesheetEntry;
import pl.chrono.reportmanager.dto.EmployeeTimesheetReport;

@Service
@RequiredArgsConstructor
public class TimesheetReportService {

  private final UserRepository userRepository;
  private final WorkLogRepository workLogRepository;
  private final ApplicationRepository applicationRepository;

  @Transactional(readOnly = true)
  public EmployeeTimesheetReport generateTimesheet(Integer employeeId, LocalDate startDate, LocalDate endDate) {
    User user = userRepository.findById(employeeId)
        .orElseThrow(() -> new IllegalArgumentException("\n" + "No user with ID found: " + employeeId));

    List<DailyTimesheetEntry> dailyEntries = new ArrayList<>();

    long totalWork = 0L;
    long totalRequired = 0L;
    long totalBalance = 0L;

    LocalDate currentDate = startDate;

    while (!currentDate.isAfter(endDate)) {
      Optional<WorkLog> workLogOpt = workLogRepository.findByUserUserIdAndStartTimeBetween(
          employeeId,
          currentDate.atStartOfDay(),
          currentDate.atTime(LocalTime.MAX)
      );

      List<Application> apps = applicationRepository.findApprovedByEmployeeAndDate(employeeId, currentDate);
      String appType = apps.isEmpty() ? null : apps.getFirst().getType().name();

      LocalTime startTime = null;
      LocalTime endTime = null;
      long effectiveWork = 0L;
      int requiredSeconds = 0;

      if (workLogOpt.isPresent()) {
        WorkLog wl = workLogOpt.get();
        startTime = wl.getStartTime().toLocalTime();
        endTime = wl.getEndTime() != null ? wl.getEndTime().toLocalTime() : null;
        effectiveWork = wl.getEffectiveWorkTime(java.time.LocalDateTime.now());
        requiredSeconds = wl.getRequiredSeconds();
      } else {
        if (user.getActivePolicy() != null) {
          requiredSeconds = user.getActivePolicy().getRequiredSecondsForDate(currentDate);
        } else {
          boolean isWeekend = currentDate.getDayOfWeek().getValue() >= 6;
          requiredSeconds = isWeekend ? 0 : 8 * 3600;
        }

        if (appType != null && isFullDayAbsence(apps.getFirst())) {
          effectiveWork = requiredSeconds;
        }
      }

      long balance = effectiveWork - requiredSeconds;

      dailyEntries.add(new DailyTimesheetEntry(
          currentDate, startTime, endTime, effectiveWork, requiredSeconds, balance, appType
      ));

      totalWork += effectiveWork;
      totalRequired += requiredSeconds;
      totalBalance += balance;

      currentDate = currentDate.plusDays(1);
    }

    return new EmployeeTimesheetReport(
        user.getUserId(),
        user.getName() + " " + user.getSurname(),
        startDate,
        endDate,
        totalWork,
        totalRequired,
        totalBalance,
        dailyEntries
    );
  }

  private boolean isFullDayAbsence(Application app) {
    String type = app.getType().name();
    return type.equals("HOLIDAY_LEAVE") || type.equals("SICK_LEAVE") || type.equals("ON_DEMAND_LEAVE");
  }
}
