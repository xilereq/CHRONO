package pl.chrono.timemanager.service;

import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.chrono.dbmanager.model.LeaveLog;
import pl.chrono.dbmanager.ports.I_TimeData;
import pl.chrono.dbmanager.repository.UserRepository;
import pl.chrono.timemanager.dto.response.LeaveLogDto;
import pl.chrono.timemanager.exception.UserException;
import pl.chrono.timemanager.exception.WorkException;
import pl.chrono.timemanager.mapper.LeaveLogMapper;

@Service
@RequiredArgsConstructor
public class LeaveLogService {
  private final I_TimeData timeData;
  private final UserRepository userRepository;
  private final LeaveLogMapper leaveLogMapper;

  @Transactional
  public LeaveLogDto startLeave(Integer userId) {
    var activeWorkLog = timeData.findActiveWorkLog(userId)
        .orElseThrow(() -> new WorkException.NoActiveWork(userId));

    if (isUserOnLeave(userId)) {
      throw new WorkException.LeaveStarted(userId);
    }

    LeaveLog leaveLog = new LeaveLog();
    leaveLog.setUser(userRepository.findById(userId).orElseThrow(
        () -> new UserException.NotFound(userId)));
    leaveLog.setStartTime(LocalDateTime.now());
    leaveLog.setWorkLog(activeWorkLog);
    LeaveLog saved = timeData.saveLeaveLog(leaveLog);
    return leaveLogMapper.toDto(saved);
  }

  @Transactional
  public LeaveLogDto endLeave(Integer userId) {
    LeaveLog leaveLog = timeData.findActiveLeaveLog(userId)
        .orElseThrow(() -> new WorkException.NoActiveLeave(userId));
    leaveLog.setEndTime(LocalDateTime.now());
    LeaveLog saved = timeData.saveLeaveLog(leaveLog);
    return leaveLogMapper.toDto(saved);
  }

  @Transactional(readOnly = true)
  public boolean isUserOnLeave(Integer userId) {
    return timeData.isUserOnLeave(userId);
  }

}
