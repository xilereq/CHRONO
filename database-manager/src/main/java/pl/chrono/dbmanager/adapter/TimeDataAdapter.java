package pl.chrono.dbmanager.adapter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import pl.chrono.dbmanager.model.LeaveLog;
import pl.chrono.dbmanager.model.WorkLog;
import pl.chrono.dbmanager.ports.I_TimeData;
import pl.chrono.dbmanager.repository.WorkLogRepository;
import pl.chrono.dbmanager.repository.LeaveLogRepository;

@Component
@RequiredArgsConstructor
public class TimeDataAdapter implements I_TimeData {
  private final WorkLogRepository workLogRepository;
  private final LeaveLogRepository leaveLogRepository;

  @Override
  public boolean isUserWorking(Integer userId) {
    return workLogRepository.existsByUser_UserIdAndEndTimeIsNull(userId);
  }

  @Override
  public WorkLog saveWorkLog(WorkLog workLog) {
    return workLogRepository.save(workLog);
  }

  @Override
  public Optional<WorkLog> findActiveWorkLog(Integer userId) {
    return workLogRepository.findByUser_UserIdAndEndTimeIsNull(userId);
  }

  @Override
  public List<WorkLog> findActiveWorkLogsBefore(LocalDateTime threshold) {
    return workLogRepository.findByEndTimeIsNullAndStartTimeBefore(threshold);
  }

  @Override
  public List<WorkLog> findUserWorkLogsAfter(Integer userId, LocalDateTime threshold) {
    return workLogRepository.findByUser_UserIdAndStartTimeAfter(userId, threshold);
  }

  @Override
  public boolean isUserOnLeave(Integer userId) {
    return leaveLogRepository.existsByUser_UserIdAndEndTimeIsNull(userId);
  }

  @Override
  public LeaveLog saveLeaveLog(LeaveLog leaveLog) {
    return leaveLogRepository.save(leaveLog);
  }

  @Override
  public Optional<LeaveLog> findActiveLeaveLog(Integer userId) {
    return leaveLogRepository.findByUser_UserIdAndEndTimeIsNull(userId);
  }
}
