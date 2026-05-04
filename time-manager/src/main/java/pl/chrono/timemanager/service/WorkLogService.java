package pl.chrono.timemanager.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.chrono.dbmanager.model.WorkLog;
import pl.chrono.dbmanager.repository.UserRepository;
import pl.chrono.dbmanager.repository.WorkLogRepository;
import pl.chrono.timemanager.dto.WorkLogDto;
import pl.chrono.timemanager.exception.NoActiveWorkException;
import pl.chrono.timemanager.exception.WorkStartedException;


@Service
@RequiredArgsConstructor
public class WorkLogService {
  private final WorkLogRepository workLogRepository;
  private final UserRepository userRepository;

  @Transactional
  public WorkLogDto startWork(Integer userId) {
    if (workLogRepository.existsByUser_UserIdAndEndTimeIsNull(userId)) {
      throw new WorkStartedException(userId);
    }

    WorkLog workLog = new WorkLog();
    workLog.setUser(userRepository.findById(userId).orElseThrow(() -> new IllegalArgumentException("User not found")));
    workLog.setStartTime(java.time.LocalDateTime.now());
    WorkLog saved = workLogRepository.save(workLog);
    return mapToDto(saved);
  }

  @Transactional
  public WorkLogDto endWork(Integer userId) {
    WorkLog workLog = workLogRepository.findByUser_UserIdAndEndTimeIsNull(userId)
      .orElseThrow(() -> new NoActiveWorkException(userId));
    workLog.setEndTime(java.time.LocalDateTime.now());
    WorkLog saved = workLogRepository.save(workLog);
    return mapToDto(saved);
  }

  @Transactional(readOnly = true)
  public boolean isUserWorking(Integer userId) {
    return workLogRepository.existsByUser_UserIdAndEndTimeIsNull(userId);
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
