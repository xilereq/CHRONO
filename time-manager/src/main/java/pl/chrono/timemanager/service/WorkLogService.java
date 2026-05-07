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
import pl.chrono.timemanager.dto.WorkLogDto;
import pl.chrono.timemanager.dto.WorkStats;
import pl.chrono.timemanager.exception.LeaveStartedException;
import pl.chrono.timemanager.exception.NoActiveWorkException;
import pl.chrono.timemanager.exception.WorkStartedException;

@Service
@RequiredArgsConstructor
public class WorkLogService {
  private final I_TimeData timeData;
  private final UserRepository userRepository;

  @Transactional
  public WorkLogDto startWork(Integer userId) {
    if (isUserWorking(userId)) {
      throw new WorkStartedException(userId);
    }

    WorkLog workLog = new WorkLog();
    workLog.setUser(userRepository.findById(userId).orElseThrow(
        () -> new IllegalArgumentException("User not found")));
    workLog.setStartTime(LocalDateTime.now());
    WorkLog saved = timeData.saveWorkLog(workLog);
    return mapToDto(saved);
  }

  @Transactional
  public WorkLogDto endWork(Integer userId) {
    WorkLog workLog = timeData.findActiveWorkLog(userId)
        .orElseThrow(() -> new NoActiveWorkException(userId));
    if (timeData.isUserOnLeave(userId)) {
      throw new LeaveStartedException(userId);
    }
    workLog.setEndTime(LocalDateTime.now());
    WorkLog saved = timeData.saveWorkLog(workLog);
    return mapToDto(saved);
  }

  @Transactional(readOnly = true)
  public boolean isUserWorking(Integer userId) {
    return timeData.isUserWorking(userId);
  }

  @Transactional(readOnly = true)
  public String getActiveWorkTime(Integer userId) {
    int duration = timeData.findActiveWorkLog(userId)
        .map(workLog -> Duration.between(workLog.getStartTime(), LocalDateTime.now()).getSeconds())
        .orElse(0L).intValue();
    int hours = duration / 3600;
    int minutes = duration % 3600 / 60;
    int seconds = duration % 60;
    return String.format("%02d:%02d:%02d", hours, minutes, seconds);
  }

  private WorkLogDto mapToDto(WorkLog workLog) {
    WorkLogDto dto = new WorkLogDto();
    dto.setWorkLogId(workLog.getWorkLogId());
    dto.setUserId(workLog.getUser().getUserId());
    dto.setStartTime(workLog.getStartTime());
    dto.setEndTime(workLog.getEndTime());
    return dto;
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
        .mapToLong(workLog -> Duration.between(workLog.getStartTime(), workLog.getEndTime()).getSeconds())
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
}

