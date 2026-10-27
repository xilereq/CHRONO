package pl.chrono.dbmanager.adapter;

import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import pl.chrono.dbmanager.model.OperationLog;
import pl.chrono.dbmanager.ports.I_OperationLogData;
import pl.chrono.dbmanager.repository.OperationLogRepository;

@Component
@RequiredArgsConstructor
public class OperationLogDataAdapter implements I_OperationLogData {
  private final OperationLogRepository operationLogRepository;

  @Override
  public boolean isLogPresent(String type, LocalDateTime start, LocalDateTime end) {
    return operationLogRepository.existsByActionAndTimestampBetween(type, start, end);
  }

  @Override
  public OperationLog saveLog(OperationLog log) {
    return operationLogRepository.save(log);
  }
}
