package pl.chrono.logmanager.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.chrono.dbmanager.repository.OperationLogRepository;
import pl.chrono.logmanager.api.I_LogService;
import pl.chrono.logmanager.dto.OperationLogDto;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LogService implements I_LogService {

  private final OperationLogRepository operationLogRepository;

  @Transactional(readOnly = true)
  public List<OperationLogDto> getAllOperationalLogs(){
    return operationLogRepository.findAll()
        .stream()
        .map(log -> new OperationLogDto(
            log.getId(),
            log.getAction(),
            log.getDetails(),
            log.getTimestamp(),
            log.getUserEmail() != null ? log.getUserEmail() : "SYSTEM"
        ))
        .collect(Collectors.toList());
  }
}
