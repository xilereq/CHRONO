package pl.chrono.timemanager.service;

import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.chrono.dbmanager.model.LeaveLog;
import pl.chrono.dbmanager.model.enums.LeaveType;
import pl.chrono.dbmanager.ports.I_TimeData;
import pl.chrono.dbmanager.repository.UserRepository;
import pl.chrono.timemanager.dto.LeaveLogDto;
import pl.chrono.timemanager.exception.WorkException;

@Service
@RequiredArgsConstructor
public class LeaveLogService {
  private final I_TimeData timeData;
  private final UserRepository userRepository;

  @Transactional
  public LeaveLogDto startLeave(Integer userId, LeaveType type) {
    var activeWorkLog = timeData.findActiveWorkLog(userId)
        .orElseThrow(() -> new WorkException.NoActiveWork(userId));

    if (isUserOnLeave(userId)) {
      throw new WorkException.LeaveStarted(userId);
    }

    LeaveLog leaveLog = new LeaveLog();
    leaveLog.setUser(userRepository.findById(userId).orElseThrow(
        () -> new IllegalArgumentException("User not found")));
    leaveLog.setStartTime(LocalDateTime.now());
    leaveLog.setType(type);
    leaveLog.setWorkLog(activeWorkLog);
    LeaveLog saved = timeData.saveLeaveLog(leaveLog);
    return mapToDto(saved);
  }

  @Transactional
  public LeaveLogDto endLeave(Integer userId) {
    LeaveLog leaveLog = timeData.findActiveLeaveLog(userId)
        .orElseThrow(() -> new WorkException.NoActiveLeave(userId));
    leaveLog.setEndTime(LocalDateTime.now());
    LeaveLog saved = timeData.saveLeaveLog(leaveLog);
    return mapToDto(saved);
  }

  @Transactional(readOnly = true)
  public boolean isUserOnLeave(Integer userId) {
    return timeData.isUserOnLeave(userId);
  }

  private LeaveLogDto mapToDto(LeaveLog leaveLog) {
    LeaveLogDto dto = new LeaveLogDto();
    dto.setLeaveLogId(leaveLog.getLeaveLogId());
    dto.setUserId(leaveLog.getUser().getUserId());
    dto.setStartTime(leaveLog.getStartTime());
    dto.setEndTime(leaveLog.getEndTime());
    dto.setLeaveType(leaveLog.getType());
    if (leaveLog.getWorkLog() != null) {
      dto.setWorkLog(leaveLog.getWorkLog().getWorkLogId());
    }
    return dto;
  }
}
