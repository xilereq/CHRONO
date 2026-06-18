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
import pl.chrono.dbmanager.model.Policy;
import pl.chrono.dbmanager.model.User;
import pl.chrono.dbmanager.model.WorkLog;
import pl.chrono.dbmanager.model.enums.WorkLocationType;
import pl.chrono.dbmanager.ports.I_TimeData;
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
  private final ApplicationEventPublisher eventPublisher;
  private final WorkTimeCalculationService workTimeCalculationService;

  @Transactional(readOnly = true)
  public List<WorkLogDto> getWorkLogsByDate(Integer userId, LocalDate date) {
    LocalDateTime startOfDay = date.atStartOfDay();
    LocalDateTime endOfDay = date.plusDays(1).atStartOfDay().minusNanos(1);

    return timeData.findWorkLogsByUserAndDate(userId, startOfDay, endOfDay)
        .stream()
        .map(workLogMapper::toDto)
        .toList();
  }

  public boolean hasUserWorkedToday(Integer userId) {
    LocalDate today = LocalDate.now();
    LocalDateTime startOfDay = today.atStartOfDay();
    LocalDateTime endOfDay = today.plusDays(1).atStartOfDay().minusNanos(1);

    return !timeData.findWorkLogsByUserAndDate(userId, startOfDay, endOfDay).isEmpty();
  }

  @EventListener
  @Transactional
  public void handleUserLoginEvent(UserLoginEvent event) {
    User user = userRepository.findById(event.userId())
        .orElseThrow(() -> new UserException.NotFound(event.userId()));
    if (user.isForcePasswordChange()) {
      return;
    }
    if (!isUserWorking(event.userId()) && !hasUserWorkedToday(event.userId())) {
      startWork(event.userId(), null);
    }
  }

  @Transactional
  public WorkLogDto startWork(Integer userId, WorkLocationType location) {
    User user = userRepository.findByIdWithPessimisticLock(userId)
        .orElseThrow(() -> new UserException.NotFound(userId));

    if (isUserWorking(userId)) {
      throw new WorkException.WorkStarted(userId);
    }

    if (hasUserWorkedToday(userId)) {
      throw new WorkException.AlreadyWorkedToday();
    }

    WorkLog workLog = new WorkLog();
    workLog.setUser(user);
    workLog.setStartTime(LocalDateTime.now());

    boolean hasHoApplication = workTimeCalculationService.hasApprovedHomeOffice(
        userId, LocalDate.now());
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

  @Transactional
  public void cancelWork(Integer userId) {
    WorkLog workLog = timeData.findActiveWorkLog(userId)
        .orElseThrow(() -> new WorkException.NoActiveWork(userId));

    if (workLog.calculateWorkTime(LocalDateTime.now()) > 900) {
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
    LocalDate workDate = workLog.getStartTime() != null ? workLog.getStartTime()
        .toLocalDate() : now.toLocalDate();
    Policy policyForDay = workLog.getUser().getPolicyForDate(workDate);

    int requiredSeconds = workTimeCalculationService.getAdjustedRequiredSeconds(
        userId, workDate, policyForDay);
    int roundingSeconds = workTimeCalculationService.getRoundingSeconds(policyForDay);
    int maxAllowedSeconds = workTimeCalculationService.getMaxAllowedSeconds(policyForDay);

    if (workTime > maxAllowedSeconds) {
      workLog.setEndTime(now);
      eventPublisher.publishEvent(new LawViolationEvent(
          userId, "VIOLATION_MAX_WORK", workLog.getWorkLogId().longValue(), "WORK_LOG",
          "Your work time has exceeded maximum work time for your policy ("
              + workTimeCalculationService.formatSecondsToTime(maxAllowedSeconds) + "). " +
              "Log has been automatically closed."
      ));
    } else if (workTime < (requiredSeconds - roundingSeconds)) {
      long missingSeconds = requiredSeconds - workTime;
      throw new WorkException.InsufficientWorkTime(
          workTimeCalculationService.formatSecondsToTime(missingSeconds));
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

    checkWeeklyWorkLimit(saved, now, policyForDay);
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
      return workTimeCalculationService.formatSecondsToTime(workTime);
    }).orElse("00:00:00");
  }

  private MonthlyHours calculateMonthlyHours(User user, LocalDate today) {
    LocalDate firstDay = today.withDayOfMonth(1);
    LocalDate lastDay = today.withDayOfMonth(today.lengthOfMonth());

    int remainingHours = 0;
    int totalMonthlyHours = 0;
    LocalDate cur = firstDay;

    while (!cur.isAfter(lastDay)) {
      Policy policyForDay = user.getPolicyForDate(cur);
      int dailySeconds = workTimeCalculationService.getBaseRequiredSeconds(cur, policyForDay);

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
            .isBefore(today.atStartOfDay())).mapToLong(workLog -> {
      Policy policy = workLog.getUser().getPolicyForDate(workLog.getStartTime().toLocalDate());
      int required = workTimeCalculationService.getBaseRequiredSeconds(
          workLog.getStartTime().toLocalDate(), policy);
      int rounding = workTimeCalculationService.getRoundingSeconds(policy);
      return workLog.getEffectiveWorkTime(workLog.getEndTime(), required, rounding);
    }).sum();

    return (int) (totalSecondsWorked / 3600);
  }

  @Transactional(readOnly = true)
  public WorkStats getMonthlyStats(Integer userId) {
    User user = userRepository.findById(userId)
        .orElseThrow(() -> new UserException.NotFound(userId));

    LocalDate today = LocalDate.now();

    MonthlyHours monthlyHours = calculateMonthlyHours(user, today);
    int totalHoursWorked = calculateTotalHoursWorked(userId, today);
    int overtimeSeconds = overtimeService.getOvertimeBalance(userId);
    int finalOvertime = Math.max(overtimeSeconds, 0);

    return new WorkStats(
        totalHoursWorked, monthlyHours.remaining(), monthlyHours.total(),
        finalOvertime
    );
  }

  @Transactional(readOnly = true)
  public String getTodayTarget(Integer userId) {
    User user = userRepository.findById(userId)
        .orElseThrow(() -> new UserException.NotFound(userId));
    LocalDate today = LocalDate.now();
    Policy policyForToday = user.getPolicyForDate(today);
    int todayRequiredSeconds = workTimeCalculationService.getAdjustedRequiredSeconds(
        userId, today, policyForToday);
    return workTimeCalculationService.formatSecondsToTime(todayRequiredSeconds);
  }

  private void checkWeeklyWorkLimit(WorkLog currentLog, LocalDateTime now, Policy policy) {
    int maxWeeklyHours = (policy != null && policy.getMaxWeeklyHours() != null) ?
        policy.getMaxWeeklyHours() : 48;
    long maxWeeklySeconds = maxWeeklyHours * 3600L;
    LocalDateTime startOfWeek = now.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        .truncatedTo(ChronoUnit.DAYS);

    long totalSecondsThisWeek = timeData.findUserWorkLogsAfter(
            currentLog.getUser().getUserId(), startOfWeek).stream()
        .mapToLong(log -> {
          Policy policyForDate = log.getUser().getPolicyForDate(log.getStartTime().toLocalDate());
          int required = workTimeCalculationService.getBaseRequiredSeconds(log.getStartTime().toLocalDate(), policyForDate);
          int rounding = workTimeCalculationService.getRoundingSeconds(policyForDate);

          LocalDateTime boundary = log.getEndTime() != null ? log.getEndTime() : now;

          return log.getEffectiveWorkTime(boundary, required, rounding);
        }).sum();

    int currentRequired = workTimeCalculationService.getBaseRequiredSeconds(currentLog.getStartTime().toLocalDate(), policy);
    int currentRounding = workTimeCalculationService.getRoundingSeconds(policy);

    long currentLogSeconds = currentLog.getEffectiveWorkTime(now, currentRequired, currentRounding);

    long secondsBeforeThisShift = totalSecondsThisWeek - currentLogSeconds;

    if (secondsBeforeThisShift <= maxWeeklySeconds && totalSecondsThisWeek > maxWeeklySeconds) {
      eventPublisher.publishEvent(new LawViolationEvent(
          currentLog.getUser().getUserId(), "VIOLATION_MAX_WEEKLY_WORK",
          currentLog.getWorkLogId().longValue(), "WORK_LOG",
          "WARNING: You have exceeded maximum weekly limit (" + maxWeeklyHours + " hours)."
      ));
    }
  }

  private record MonthlyHours(int total, int remaining) {
  }
}

