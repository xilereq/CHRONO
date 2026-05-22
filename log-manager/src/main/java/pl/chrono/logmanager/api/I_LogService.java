package pl.chrono.logmanager.api;

import pl.chrono.logmanager.dto.OperationLogDto;
import java.util.List;

public interface I_LogService {
  List<OperationLogDto> getAllOperationalLogs();
}
