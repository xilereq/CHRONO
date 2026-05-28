package pl.chrono.timemanager.service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.chrono.dbmanager.model.Application;
import pl.chrono.dbmanager.model.Policy;
import pl.chrono.dbmanager.model.User;
import pl.chrono.dbmanager.model.WorkLog;
import pl.chrono.dbmanager.ports.I_TimeData;
import pl.chrono.dbmanager.repository.UserRepository;
import pl.chrono.dbmanager.repository.ApplicationRepository;
import pl.chrono.timemanager.dto.response.WorkLogDto;
import pl.chrono.timemanager.dto.response.WorkStats;
import pl.chrono.timemanager.exception.WorkException;
import pl.chrono.timemanager.mapper.WorkLogMapper;

@Service
@RequiredArgsConstructor
public class WorkLogService {
  private final I_TimeData timeData;
  private final UserRepository userRepository;
  private final WorkLogMapper workLogMapper;
  private final OvertimeService overtimeService;
  private final ApplicationRepository applicationRepository;

  @Transactional
  public WorkLogDto startWork(Integer userId) {
    if (isUserWorking(userId)) {
      throw new WorkException.WorkStarted(userId);
    }

    WorkLog workLog = new WorkLog();
    workLog.setUser(userRepository.findById(userId).orElseThrow(
        () -> new IllegalArgumentException("User not found")));
    workLog.setStartTime(LocalDateTime.now());
    WorkLog saved = timeData.saveWorkLog(workLog);
    return workLogMapper.toDto(saved);
  }

  @Transactional(noRollbackFor = WorkException.ExcessiveWorkTime.class)
  public WorkLogDto endWork(Integer userId) {
    WorkLog workLog = timeData.findActiveWorkLog(userId)
        .orElseThrow(() -> new WorkException.NoActiveWork(userId));
    if (timeData.isUserOnLeave(userId)) {
      throw new WorkException.LeaveStarted(userId);
    }

    LocalDateTime now = LocalDateTime.now();
    long workTime = workLog.calculateWorkTime(now);

    int requiredSeconds = workLog.getRequiredSeconds();
    int roundingSeconds = workLog.getRoundingSeconds();
    int maxAllowedSeconds = workLog.getMaxAllowedSeconds();
    if (workTime > maxAllowedSeconds) {
      workLog.forceClose(now);
      timeData.saveWorkLog(workLog);
      throw new WorkException.ExcessiveWorkTime(requiredSeconds, maxAllowedSeconds);
    } else {
      if (workTime < (requiredSeconds - roundingSeconds)) {
        long missingSeconds = requiredSeconds - workTime;
        throw new WorkException.InsufficientWorkTime(formatSecondsToTime(missingSeconds));
      }
      workLog.setEndTime(LocalDateTime.now());
    }
    WorkLog saved = timeData.saveWorkLog(workLog);
    return workLogMapper.toDto(saved);
  }

  @Transactional(readOnly = true)
  public boolean isUserWorking(Integer userId) {
    return timeData.isUserWorking(userId);
  }

  @Transactional(readOnly = true)
  public String getActiveWorkTime(Integer userId) {
    return timeData.findActiveWorkLog(userId)
        .map(workLog -> {
          long workTime = workLog.calculateWorkTime(LocalDateTime.now());
          return formatSecondsToTime(workTime);
        })
        .orElse("00:00:00");
  }

  private boolean defaultIsWorkDay(LocalDate date) {
    DayOfWeek day = date.getDayOfWeek();
    return day != DayOfWeek.SATURDAY && day != DayOfWeek.SUNDAY;
  }

  private record MonthlyHours(int total, int remaining) {}

  private MonthlyHours calculateMonthlyHours(Policy policy, LocalDate today) {
    LocalDate firstDay = today.withDayOfMonth(1);
    LocalDate lastDay = today.withDayOfMonth(today.lengthOfMonth());

    int remainingHours = 0;
    int totalMonthlyHours = 0;
    LocalDate cur = firstDay;

    while (!cur.isAfter(lastDay)) {
      int dailySeconds = policy != null
          ? policy.getRequiredSecondsForDate(cur)
          : defaultIsWorkDay(cur) ? 8 * 3600 : 0;

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

    long totalSecondsWorked = timeData.findUserWorkLogsAfter(userId, startOfMonth)
        .stream()
        .filter(workLog -> workLog.getEndTime() != null
            && workLog.getEndTime().isBefore(today.atStartOfDay()))
        .mapToLong(workLog -> workLog.getEffectiveWorkTime(workLog.getEndTime()))
        .sum();

    return (int) (totalSecondsWorked / 3600);
  }

  @Transactional(readOnly = true)
  public WorkStats getMonthlyStats(Integer userId) {
    User user = userRepository.findById(userId)
        .orElseThrow(() -> new IllegalArgumentException("User not found"));

    LocalDate today = LocalDate.now();
    Policy policy = user.getActivePolicy();

    MonthlyHours monthlyHours = calculateMonthlyHours(policy, today);

    int totalHoursWorked = calculateTotalHoursWorked(userId, today);

    int overtimeSeconds = overtimeService.getOvertimeBalance(userId);

    return new WorkStats(
        totalHoursWorked,
        monthlyHours.remaining(),
        monthlyHours.total(),
        overtimeSeconds
    );
  }

  private String formatSecondsToTime(long totalSeconds) {
    long hours = totalSeconds / 3600;
    long minutes = totalSeconds % 3600 / 60;
    long seconds = totalSeconds % 60;
    return String.format("%02d:%02d:%02d", hours, minutes, seconds);
  }

  @Transactional(readOnly = true)
  public String getTodayTarget(Integer userId) {
    User user = userRepository.findById(userId)
        .orElseThrow(() -> new IllegalArgumentException("User not found"));

    LocalDate today = LocalDate.now();
    Policy policy = user.getActivePolicy();

    int baseRequiredSeconds = policy != null ? policy.getRequiredSecondsForDate(today) : defaultIsWorkDay(today) ? 8 * 3600 : 0;

    List<Application> todaysApps = applicationRepository.findApprovedByEmployeeAndDate(userId, today);

    for (Application app : todaysApps) {
      int duration = app.getDurationSeconds() != null ? app.getDurationSeconds() : 0;

      switch (app.getType()) {
        case EARLY_EXIT:
        case OVERTIME_TAKE:
          baseRequiredSeconds -= duration;
          break;
        case OVERTIME_EARN:
          baseRequiredSeconds += duration;
          break;
        default:
          break;
      }
    }

    baseRequiredSeconds = Math.max(0, baseRequiredSeconds);

    return formatSecondsToTime(baseRequiredSeconds);
  }
}

