package pl.chrono.timemanager.service;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.chrono.dbmanager.model.Application;
import pl.chrono.dbmanager.model.Policy;
import pl.chrono.dbmanager.model.User;
import pl.chrono.dbmanager.model.WorkLog;
import pl.chrono.dbmanager.model.enums.ApplicationType;
import pl.chrono.dbmanager.model.enums.WorkLocationType;
import pl.chrono.dbmanager.ports.I_TimeData;
import pl.chrono.dbmanager.repository.ApplicationRepository;
import pl.chrono.dbmanager.repository.UserRepository;
import pl.chrono.timemanager.dto.response.WorkLogDto;
import pl.chrono.timemanager.dto.response.WorkStats;
import pl.chrono.timemanager.event.LawViolationEvent;
import pl.chrono.timemanager.exception.UserException;
import pl.chrono.timemanager.exception.WorkException;
import pl.chrono.timemanager.mapper.WorkLogMapper;
import pl.chrono.usermanager.event.UserLoginEvent;

@Service
@RequiredArgsConstructor
public class WorkLogService {
  private final I_TimeData timeData;
  private final UserRepository userRepository;
  private final WorkLogMapper workLogMapper;
  private final OvertimeService overtimeService;
  private final ApplicationRepository applicationRepository;
  private final ApplicationEventPublisher eventPublisher;

  @Transactional(readOnly = true)
  public List<WorkLogDto> getWorkLogsByDate(Integer userId, LocalDate date) {
    LocalDateTime startOfDay = date.atStartOfDay();
    LocalDateTime endOfDay = date.plusDays(1).atStartOfDay().minusNanos(1);

    return timeData.findWorkLogsByUserAndDate(userId, startOfDay, endOfDay)
        .stream()
        .map(workLogMapper::toDto)
        .toList();
  }

  @EventListener
  @Transactional
  public void handleUserLoginEvent(UserLoginEvent event) {
    if (!isUserWorking(event.userId())) {
        startWork(event.userId(), null);
    }
  }

  @Transactional
  public WorkLogDto startWork(Integer userId, WorkLocationType location) {
    if (isUserWorking(userId)) {
      throw new WorkException.WorkStarted(userId);
    }

    WorkLog workLog = new WorkLog();
    workLog.setUser(userRepository.findById(userId)
        .orElseThrow(() -> new UserException.NotFound(userId)));
    workLog.setStartTime(LocalDateTime.now());

    boolean hasHoApplication = isHomeOffice(userId);
    if (location != null) {
      if (location == WorkLocationType.HOME_OFFICE && !hasHoApplication) {
        throw new WorkException.NoHomeOfficeApplication();
      }
    } else {
      location = hasHoApplication ? WorkLocationType.HOME_OFFICE : WorkLocationType.OFFICE;
    }
    workLog.setLocationType(location);

    WorkLog saved = timeData.saveWorkLog(workLog);
    return workLogMapper.toDto(saved);
  }

  private boolean isHomeOffice(Integer userId) {
    return applicationRepository.findApprovedByEmployeeAndDate(userId, LocalDate.now())
        .stream()
        .anyMatch(app -> app.getType() == ApplicationType.HOME_OFFICE);
  }

  @Transactional
  public void cancelWork(Integer userId) {
    WorkLog workLog = timeData.findActiveWorkLog(userId)
        .orElseThrow(() -> new WorkException.NoActiveWork(userId));

    long minutesPassed = Duration.between(workLog.getStartTime(), LocalDateTime.now()).toMinutes();
    if (minutesPassed > 15) {
      throw new WorkException.CancellationExpired();
    }

    timeData.deleteWorkLog(workLog);
  }

  @Transactional()
  public WorkLogDto endWork(Integer userId) {
    WorkLog workLog = timeData.findActiveWorkLog(userId)
        .orElseThrow(() -> new WorkException.NoActiveWork(userId));
    if (timeData.isUserOnLeave(userId)) {
      throw new WorkException.LeaveStarted(userId);
    }

    LocalDateTime now = LocalDateTime.now();
    long workTime = workLog.calculateWorkTime(now);

    int requiredSeconds = calculateAdjustedRequiredSeconds(
        userId, now.toLocalDate(),
        workLog.getRequiredSeconds()
    );
    int roundingSeconds = workLog.getRoundingSeconds();
    int maxAllowedSeconds = workLog.getMaxAllowedSeconds();

    if (workTime > maxAllowedSeconds) {
      workLog.forceClose(now);
      eventPublisher.publishEvent(new LawViolationEvent(
          userId, "VIOLATION_MAX_WORK", workLog.getWorkLogId().longValue(), "WORK_LOG",
          "Your work time has exceeded maximum work time for your policy (" + formatSecondsToTime(
              maxAllowedSeconds) + "). Log has been automatically closed."
      ));
    } else if (workTime < (requiredSeconds - roundingSeconds)) {
      long missingSeconds = requiredSeconds - workTime;
      throw new WorkException.InsufficientWorkTime(formatSecondsToTime(missingSeconds));
    } else {
      workLog.setEndTime(now);
    }

    WorkLog saved = timeData.saveWorkLog(workLog);

    timeData.findLastClosedWorkLog(userId).ifPresent(lastLog -> {
      if (!lastLog.getWorkLogId().equals(saved.getWorkLogId())) {
        long hoursPassed = Duration.between(lastLog.getEndTime(), saved.getStartTime()).toHours();
        if (hoursPassed < 11) {
          eventPublisher.publishEvent(new LawViolationEvent(
              userId, "VIOLATION_REST", saved.getWorkLogId().longValue(), "WORK_LOG",
              "WARNING: You have worked less than 11 hours since your last shift (hours passed: " + hoursPassed + ")."
          ));
        }
      }
    });
    checkWeeklyWorkLimit(saved, now, workLog.getUser().getActivePolicy());
    return workLogMapper.toDto(saved);
  }

  @Transactional(readOnly = true)
  public boolean isUserWorking(Integer userId) {
    return timeData.isUserWorking(userId);
  }

  @Transactional(readOnly = true)
  public String getActiveWorkTime(Integer userId) {
    return timeData.findActiveWorkLog(userId).map(workLog -> {
      long workTime = workLog.calculateWorkTime(LocalDateTime.now());
      return formatSecondsToTime(workTime);
    }).orElse("00:00:00");
  }

  private boolean defaultIsWorkDay(LocalDate date) {
    DayOfWeek day = date.getDayOfWeek();
    return day != DayOfWeek.SATURDAY && day != DayOfWeek.SUNDAY;
  }

  private record MonthlyHours(int total, int remaining) {
  }

  private MonthlyHours calculateMonthlyHours(Policy policy, LocalDate today) {
    LocalDate firstDay = today.withDayOfMonth(1);
    LocalDate lastDay = today.withDayOfMonth(today.lengthOfMonth());

    int remainingHours = 0;
    int totalMonthlyHours = 0;
    LocalDate cur = firstDay;

    while (!cur.isAfter(lastDay)) {
      int dailySeconds = policy != null ? policy.getRequiredSecondsForDate(cur) : defaultIsWorkDay(
          cur) ? 8 * 3600 : 0;

      int dailyHours = dailySeconds / 3600;
      totalMonthlyHours += dailyHours;

      if (!cur.isBefore(today)) {
        remainingHours += dailyHours;
      }
      cur = cur.plusDays(1);
    }
    return new MonthlyHours(totalMonthlyHours, remainingHours);
  }

  private int calculateTotalHoursWorked(Integer userId, LocalDate today) {
    LocalDateTime startOfMonth = today.withDayOfMonth(1).atStartOfDay();

    long totalSecondsWorked = timeData.findUserWorkLogsAfter(userId, startOfMonth).stream().filter(
            workLog -> workLog.getEndTime() != null && workLog.getEndTime()
                .isBefore(today.atStartOfDay()))
        .mapToLong(workLog -> workLog.getEffectiveWorkTime(workLog.getEndTime())).sum();

    return (int) (totalSecondsWorked / 3600);
  }

  @Transactional(readOnly = true)
  public WorkStats getMonthlyStats(Integer userId) {
    User user = userRepository.findById(userId)
        .orElseThrow(() -> new UserException.NotFound(userId));

    LocalDate today = LocalDate.now();
    Policy policy = user.getActivePolicy();

    MonthlyHours monthlyHours = calculateMonthlyHours(policy, today);

    int totalHoursWorked = calculateTotalHoursWorked(userId, today);

    int overtimeSeconds = overtimeService.getOvertimeBalance(userId);

    return new WorkStats(
        totalHoursWorked, monthlyHours.remaining(), monthlyHours.total(),
        overtimeSeconds
    );
  }

  private String formatSecondsToTime(long totalSeconds) {
    long hours = totalSeconds / 3600;
    long minutes = totalSeconds % 3600 / 60;
    long seconds = totalSeconds % 60;
    return String.format("%02d:%02d:%02d", hours, minutes, seconds);
  }

  private int calculateAdjustedRequiredSeconds(Integer userId, LocalDate date,
                                               int baseRequiredSeconds) {
    List<Application> applications = applicationRepository.findApprovedByEmployeeAndDate(
        userId, date);
    int adjustedRequiredSeconds = baseRequiredSeconds;

    for (Application app : applications) {
      int duration = app.getDurationSeconds() != null ? app.getDurationSeconds() : 0;
      switch (app.getType()) {
        case EARLY_EXIT:
        case OVERTIME_TAKE:
          adjustedRequiredSeconds -= duration;
          break;
        case OVERTIME_EARN:
          adjustedRequiredSeconds += duration;
          break;
        default:
          break;
      }
    }
    return Math.max(0, adjustedRequiredSeconds);
  }

  @Transactional(readOnly = true)
  public String getTodayTarget(Integer userId) {
    User user = userRepository.findById(userId)
        .orElseThrow(() -> new UserException.NotFound(userId));
    LocalDate today = LocalDate.now();

    int baseRequiredSeconds = user.getActivePolicy() != null ? user.getActivePolicy()
        .getRequiredSecondsForDate(today) : defaultIsWorkDay(today) ? 8 * 3600 : 0;
    int todayRequiredSeconds = calculateAdjustedRequiredSeconds(userId, today, baseRequiredSeconds);
    return formatSecondsToTime(todayRequiredSeconds);
  }

  private void checkWeeklyWorkLimit(WorkLog currentLog, LocalDateTime now, Policy policy) {
    int maxWeeklyHours = (policy != null && policy.getMaxWeeklyHours() != null) ?
        policy.getMaxWeeklyHours() : 48;
    long maxWeeklySeconds = maxWeeklyHours * 3600L;
    LocalDateTime startOfWeek = now.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        .truncatedTo(ChronoUnit.DAYS);

    long totalSecondsThisWeek = timeData.findUserWorkLogsAfter(
            currentLog.getUser().getUserId(), startOfWeek).stream()
        .mapToLong(log -> log.getEffectiveWorkTime(now)).sum();

    long currentLogSeconds = currentLog.getEffectiveWorkTime(now);
    long secondsBeforeThisShift = totalSecondsThisWeek - currentLogSeconds;

    if (secondsBeforeThisShift <= maxWeeklySeconds && totalSecondsThisWeek > maxWeeklySeconds) {
      eventPublisher.publishEvent(new LawViolationEvent(
          currentLog.getUser().getUserId(), "VIOLATION_MAX_WEEKLY_WORK",
          currentLog.getWorkLogId().longValue(), "WORK_LOG",
          "WARNING: You have exceeded maximum weekly limit (" + maxWeeklyHours + " hours)."
      ));

    }
  }
}

