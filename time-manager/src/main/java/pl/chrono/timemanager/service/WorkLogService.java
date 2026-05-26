package pl.chrono.timemanager.service;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.chrono.dbmanager.model.WorkLog;
import pl.chrono.dbmanager.ports.I_TimeData;
import pl.chrono.dbmanager.repository.UserRepository;
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

  @Transactional
  public WorkLogDto endWork(Integer userId) {
    WorkLog workLog = timeData.findActiveWorkLog(userId)
        .orElseThrow(() -> new WorkException.NoActiveWork(userId));
    if (timeData.isUserOnLeave(userId)) {
      throw new WorkException.LeaveStarted(userId);
    }
    workLog.setEndTime(LocalDateTime.now());
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
          LocalDateTime now = LocalDateTime.now();
          long totalSeconds = Duration.between(workLog.getStartTime(), now).getSeconds();
          long breakSeconds = workLog.calculateTotalBreakSeconds(now);
          long effectiveSeconds = Math.max(0, totalSeconds - breakSeconds);
          return formatSecondsToTime(effectiveSeconds);
        })
        .orElse("00:00:00");
  }

  private boolean isWorkDay(LocalDate date) {
    DayOfWeek day = date.getDayOfWeek();
    return day != DayOfWeek.SATURDAY && day != DayOfWeek.SUNDAY;
  }

  public WorkStats getMonthlyStats(Integer userId) {
    LocalDate today = LocalDate.now();
    LocalDate firstDay = today.withDayOfMonth(1);
    LocalDate lastDay = today.withDayOfMonth(today.lengthOfMonth());

    int remainingHours = 0;
    int totalMonthlyHours = 0;
    LocalDate cur = firstDay;

    while (!cur.isAfter(lastDay)) {
      if (isWorkDay(cur)) {
        totalMonthlyHours += 8;
        if (!cur.isBefore(today)) {
          remainingHours += 8;
        }
      }
        cur = cur.plusDays(1);
    }

    LocalDateTime startOfMonth = today.withDayOfMonth(1).atStartOfDay();
    long totalSecondsWorked = timeData.findUserWorkLogsAfter(userId, startOfMonth)
        .stream()
        .filter(workLog -> workLog.getEndTime() != null
            && workLog.getEndTime().isBefore(today.atStartOfDay()))
        .mapToLong(workLog -> {
          long breakSeconds = workLog.calculateTotalBreakSeconds(workLog.getEndTime());
          long workSeconds = Duration.between(workLog.getStartTime(),
              workLog.getEndTime()).getSeconds();
          return Math.max(0, workSeconds - breakSeconds);
        })
        .sum();

    int totalHoursWorked = (int) (totalSecondsWorked / 3600);

    int overtimeSeconds = userRepository.findById(userId)
        .map(user -> user.getOvertimeBalance() != null ? user.getOvertimeBalance() : 0)
        .orElse(0);


    return new WorkStats(
        totalHoursWorked,
        remainingHours,
        totalMonthlyHours,
        overtimeSeconds
    );

  }

  private String formatSecondsToTime(long totalSeconds) {
    long hours = totalSeconds / 3600;
    long minutes = (totalSeconds % 3600) / 60;
    long seconds = totalSeconds % 60;
    return String.format("%02d:%02d:%02d", hours, minutes, seconds);
  }
}

