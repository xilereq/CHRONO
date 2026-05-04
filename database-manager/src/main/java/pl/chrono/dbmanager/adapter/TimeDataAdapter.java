package pl.chrono.dbmanager.adapter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import pl.chrono.dbmanager.model.WorkLog;
import pl.chrono.dbmanager.ports.I_TimeData;
import pl.chrono.dbmanager.repository.WorkLogRepository;

@Component
@RequiredArgsConstructor
public class TimeDataAdapter implements I_TimeData {
  private final WorkLogRepository workLogRepository;

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
}
