package pl.chrono.timemanager.service;

import java.time.Duration;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.chrono.dbmanager.model.WorkLog;
import pl.chrono.dbmanager.ports.I_TimeData;
import pl.chrono.dbmanager.repository.UserRepository;
import pl.chrono.timemanager.dto.WorkLogDto;
import pl.chrono.timemanager.exception.NoActiveWorkException;
import pl.chrono.timemanager.exception.WorkStartedException;

@Service
@RequiredArgsConstructor
public class WorkLogService {
  private final I_TimeData timeData;
  private final UserRepository userRepository;

  @Value("${chrono.max-work-hours:12}")
  private int maxWorkHours;

  @Transactional
  public WorkLogDto startWork(Integer userId) {
    if (isUserWorking(userId)) {
      throw new WorkStartedException(userId);
    }

    WorkLog workLog = new WorkLog();
    workLog.setUser(userRepository.findById(userId).orElseThrow(() -> new IllegalArgumentException("User not found")));
    workLog.setStartTime(java.time.LocalDateTime.now());
    WorkLog saved = timeData.saveWorkLog(workLog);
    return mapToDto(saved);
  }

  @Transactional
  public WorkLogDto endWork(Integer userId) {
    WorkLog workLog = timeData.findActiveWorkLog(userId)
      .orElseThrow(() -> new NoActiveWorkException(userId));
    workLog.setEndTime(java.time.LocalDateTime.now());
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
    int minutes = (duration % 3600) / 60;
    int seconds = duration % 60;
    return String.format("%02d:%02d:%02d", hours, minutes, seconds);
  }

  @Scheduled(fixedRate = 300000)
  @Transactional
  public void closeTooLongWorkLogs() {
    LocalDateTime threshold = LocalDateTime.now().minusHours(maxWorkHours);
    timeData.findActiveWorkLogsBefore(threshold).forEach(workLog -> {
      workLog.setEndTime(workLog.getStartTime().plusHours(maxWorkHours));
      timeData.saveWorkLog(workLog);
    });
  }

  private WorkLogDto mapToDto(WorkLog workLog) {
    WorkLogDto dto = new WorkLogDto();
    dto.setWorkLogId(workLog.getWorkLogId());
    dto.setUserId(workLog.getUser().getUserId());
    dto.setStartTime(workLog.getStartTime());
    dto.setEndTime(workLog.getEndTime());
    return dto;
  }
}
