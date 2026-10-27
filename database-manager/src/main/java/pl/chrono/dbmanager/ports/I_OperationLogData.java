package pl.chrono.dbmanager.ports;

import java.time.LocalDateTime;
import pl.chrono.dbmanager.model.OperationLog;

public interface I_OperationLogData {
  boolean isLogPresent(String type, LocalDateTime start, LocalDateTime end);

  OperationLog saveLog(OperationLog log);
}
