package pl.chrono.reportmanager.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.chrono.dbmanager.model.Application;
import pl.chrono.dbmanager.model.Policy;
import pl.chrono.dbmanager.model.User;
import pl.chrono.dbmanager.model.WorkLog;
import pl.chrono.dbmanager.repository.ApplicationRepository;
import pl.chrono.dbmanager.repository.UserRepository;
import pl.chrono.dbmanager.repository.WorkLogRepository;
import pl.chrono.reportmanager.dto.DailyTimesheetEntry;
import pl.chrono.reportmanager.dto.EmployeeTimesheetReport;
import pl.chrono.timemanager.service.WorkTimeCalculationService;

@Service
@RequiredArgsConstructor
public class TimesheetReportService {

  private final UserRepository userRepository;
  private final WorkLogRepository workLogRepository;
  private final ApplicationRepository applicationRepository;
  private final WorkTimeCalculationService workTimeCalculationService;

  @Transactional(readOnly = true)
  public EmployeeTimesheetReport generateTimesheet(Integer employeeId, LocalDate startDate, LocalDate endDate) {
    User user = userRepository.findById(employeeId)
        .orElseThrow(() -> new IllegalArgumentException("No user with ID found: " + employeeId));

    List<DailyTimesheetEntry> dailyEntries = new ArrayList<>();

    long totalWork = 0L;
    long totalRequired = 0L;
    long totalBalance = 0L;
    long totalNight = 0L;

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
      long nightSeconds = 0L;

      Policy policyForDay = user.getPolicyForDate(currentDate);
      requiredSeconds = workTimeCalculationService.getBaseRequiredSeconds(currentDate, policyForDay);

      if (workLogOpt.isPresent()) {
        WorkLog wl = workLogOpt.get();
        startTime = wl.getStartTime().toLocalTime();
        endTime = wl.getEndTime() != null ? wl.getEndTime().toLocalTime() : null;
        int roundingSeconds = workTimeCalculationService.getRoundingSeconds(policyForDay);
        effectiveWork = wl.getEffectiveWorkTime(LocalDateTime.now(), requiredSeconds, roundingSeconds);

        if (wl.getEndTime() != null) {
          nightSeconds = calculateNightSeconds(wl.getStartTime(), wl.getEndTime());
        }

      } else {
        if (appType != null && isFullDayAbsence(apps.getFirst())) {
          effectiveWork = requiredSeconds;
        }
      }

      long balance = effectiveWork - requiredSeconds;

      dailyEntries.add(new DailyTimesheetEntry(
          currentDate, startTime, endTime, effectiveWork, requiredSeconds, balance, nightSeconds, appType
      ));

      totalWork += effectiveWork;
      totalRequired += requiredSeconds;
      totalBalance += balance;
      totalNight += nightSeconds;

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
        totalNight,
        dailyEntries
    );
  }

  private long calculateNightSeconds(LocalDateTime start, LocalDateTime end) {
    long nightSeconds = 0;
    LocalDateTime current = start;

    while (current.isBefore(end)) {
      LocalDate date = current.toLocalDate();

      LocalDateTime nightStart1 = date.atStartOfDay();
      LocalDateTime nightEnd1 = date.atTime(6, 0);

      LocalDateTime nightStart2 = date.atTime(22, 0);
      LocalDateTime nightEnd2 = date.plusDays(1).atStartOfDay();

      nightSeconds += getOverlapSeconds(current, end, nightStart1, nightEnd1);
      nightSeconds += getOverlapSeconds(current, end, nightStart2, nightEnd2);

      current = date.plusDays(1).atStartOfDay();
    }
    return nightSeconds;
  }

  private long getOverlapSeconds(LocalDateTime start1, LocalDateTime end1, LocalDateTime start2, LocalDateTime end2) {
    LocalDateTime overlapStart = start1.isAfter(start2) ? start1 : start2;
    LocalDateTime overlapEnd = end1.isBefore(end2) ? end1 : end2;

    if (overlapStart.isBefore(overlapEnd)) {
      return Duration.between(overlapStart, overlapEnd).getSeconds();
    }
    return 0;
  }

  private boolean isFullDayAbsence(Application app) {
    String type = app.getType().name();
    return type.equals("HOLIDAY_LEAVE") || type.equals("SICK_LEAVE") || type.equals("ON_DEMAND_LEAVE");
  }
}
