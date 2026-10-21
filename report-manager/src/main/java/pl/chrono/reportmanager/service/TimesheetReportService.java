package pl.chrono.reportmanager.service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.chrono.dbmanager.model.Application;
import pl.chrono.dbmanager.model.Policy;
import pl.chrono.dbmanager.model.User;
import pl.chrono.dbmanager.model.WorkLog;
import pl.chrono.dbmanager.model.enums.ApplicationType;
import pl.chrono.dbmanager.repository.ApplicationRepository;
import pl.chrono.dbmanager.repository.UserRepository;
import pl.chrono.dbmanager.repository.WorkLogRepository;
import pl.chrono.reportmanager.dto.DailyTimesheetEntry;
import pl.chrono.reportmanager.dto.EmployeeTimesheetReport;
import pl.chrono.reportmanager.exception.ReportUserException;
import pl.chrono.timemanager.service.WorkTimeCalculationService;

@Service
@RequiredArgsConstructor
public class TimesheetReportService {

  private static final Set<ApplicationType> FULL_DAY_ABSENCES =
      EnumSet.of(ApplicationType.HOLIDAY_LEAVE, ApplicationType.SICK_LEAVE);

  private final UserRepository userRepository;
  private final WorkLogRepository workLogRepository;
  private final ApplicationRepository applicationRepository;
  private final WorkTimeCalculationService workTimeCalculationService;

  @Transactional(readOnly = true)
  public EmployeeTimesheetReport generateTimesheet(Integer employeeId, LocalDate startDate, LocalDate endDate) {
    User user = userRepository.findById(employeeId)
        .orElseThrow(() -> new ReportUserException.NotFound(employeeId));

    Map<LocalDate, List<WorkLog>> workLogsByDate = workLogRepository
        .findByUser_UserIdAndPendingCorrectionFalseAndStartTimeBetweenOrderByStartTimeAsc(
            employeeId, startDate.atStartOfDay(), endDate.atTime(LocalTime.MAX))
        .stream()
        .collect(Collectors.groupingBy(wl -> wl.getStartTime().toLocalDate()));

    LocalDateTime now = LocalDateTime.now();
    List<DailyTimesheetEntry> dailyEntries = new ArrayList<>();

    for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
      List<WorkLog> dayLogs = workLogsByDate.getOrDefault(date, List.of());
      dailyEntries.add(buildDailyEntry(user, date, dayLogs, now));
    }

    return new EmployeeTimesheetReport(
        user.getUserId(),
        user.getName() + " " + user.getSurname(),
        startDate,
        endDate,
        dailyEntries.stream().mapToLong(DailyTimesheetEntry::effectiveWorkSeconds).sum(),
        dailyEntries.stream().mapToLong(DailyTimesheetEntry::requiredSeconds).sum(),
        dailyEntries.stream().mapToLong(DailyTimesheetEntry::balanceSeconds).sum(),
        dailyEntries.stream().mapToLong(DailyTimesheetEntry::nightSeconds).sum(),
        dailyEntries
    );
  }

  private DailyTimesheetEntry buildDailyEntry(User user, LocalDate date, List<WorkLog> dayLogs,
                                              LocalDateTime now) {
    List<Application> apps =
        applicationRepository.findApprovedByEmployeeAndDate(user.getUserId(), date);
    Application app = apps.isEmpty() ? null : apps.getFirst();

    Policy policyForDay = user.getPolicyForDate(date);
    int requiredSeconds = workTimeCalculationService.getBaseRequiredSeconds(date, policyForDay);

    LocalTime startTime = null;
    LocalTime endTime = null;
    long effectiveWork = 0L;
    long nightSeconds = 0L;

    if (!dayLogs.isEmpty()) {
      int roundingSeconds = workTimeCalculationService.getRoundingSeconds(policyForDay);
      startTime = dayLogs.getFirst().getStartTime().toLocalTime();
      LocalDateTime lastEnd = dayLogs.getLast().getEndTime();
      endTime = lastEnd != null ? lastEnd.toLocalTime() : null;

      for (WorkLog wl : dayLogs) {
        effectiveWork += wl.getEffectiveWorkTime(now, requiredSeconds, roundingSeconds);
        if (wl.getEndTime() != null) {
          nightSeconds += calculateNightSeconds(wl.getStartTime(), wl.getEndTime());
        }
      }
    } else if (app != null && FULL_DAY_ABSENCES.contains(app.getType())) {
      effectiveWork = requiredSeconds;
    }

    return new DailyTimesheetEntry(
        date,
        startTime,
        endTime,
        effectiveWork,
        requiredSeconds,
        effectiveWork - requiredSeconds,
        nightSeconds,
        app != null ? app.getType().name() : null
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
}
