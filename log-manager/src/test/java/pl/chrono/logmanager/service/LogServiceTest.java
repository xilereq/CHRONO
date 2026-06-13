package pl.chrono.logmanager.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pl.chrono.dbmanager.model.OperationLog;
import pl.chrono.dbmanager.repository.OperationLogRepository;
import pl.chrono.logmanager.dto.OperationLogDto;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LogServiceTest {

  @Mock
  private OperationLogRepository operationLogRepository;

  @InjectMocks
  private LogService logService;

  @Test
  void getAllOperationalLogs_ShouldMapLogsCorrectly() {
    // Arrange
    OperationLog log1 = new OperationLog();
    log1.setId(1L);
    log1.setAction("LOGIN");
    log1.setDetails("Success");
    log1.setTimestamp(LocalDateTime.now());
    log1.setUserEmail("test@test.pl");

    OperationLog logSystem = new OperationLog();
    logSystem.setId(2L);
    logSystem.setAction("CRON");
    logSystem.setDetails("Job executed");
    logSystem.setTimestamp(LocalDateTime.now());
    logSystem.setUserEmail(null);

    when(operationLogRepository.findAll()).thenReturn(List.of(log1, logSystem));

    List<OperationLogDto> result = logService.getAllOperationalLogs();

    assertEquals(2, result.size());

    assertEquals(1L, result.get(0).logID());
    assertEquals("test@test.pl", result.get(0).userEmail());

    assertEquals(2L, result.get(1).logID());
    assertEquals("SYSTEM", result.get(1).userEmail());
  }
}
