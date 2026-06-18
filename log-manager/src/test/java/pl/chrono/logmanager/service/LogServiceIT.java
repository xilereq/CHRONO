package pl.chrono.logmanager.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import pl.chrono.dbmanager.model.OperationLog;
import pl.chrono.dbmanager.repository.OperationLogRepository;
import pl.chrono.logmanager.TestConfig;
import pl.chrono.logmanager.dto.OperationLogDto;

import java.time.LocalDateTime;
import java.util.List;

@SpringBootTest(classes = TestConfig.class)
@Transactional
@ActiveProfiles("local")
class LogServiceIT {

  @Autowired
  private LogService logService;

  @Autowired
  private OperationLogRepository operationLogRepository;

  @BeforeEach
  void setUp() {
    operationLogRepository.deleteAll();
  }

  @Test
  void getAllOperationalLogs_returnsAllSavedLogsMappedToDto() {
    persistLog("LOGIN", "Użytkownik zalogował się poprawnie", "jan@test.pl", LocalDateTime.of(2026, 6, 18, 10, 0));
    persistLog("PASSWORD_CHANGE", "Zmieniono hasło", "anna@test.pl", LocalDateTime.of(2026, 6, 18, 12, 30));

    List<OperationLogDto> result = logService.getAllOperationalLogs();

    assertThat(result).hasSize(2);

    assertThat(result)
        .extracting("action")
        .containsExactlyInAnyOrder("LOGIN", "PASSWORD_CHANGE");
  }

  @Test
  void getAllOperationalLogs_mapsNullEmailToSystemFallback() {

    persistLog("DATA_CLEANUP", "Zakończono proces czyszczenia bazy", null, LocalDateTime.now());

    List<OperationLogDto> result = logService.getAllOperationalLogs();

    assertThat(result).hasSize(1);
    OperationLogDto dto = result.get(0);

    assertThat(dto).extracting("userEmail").isEqualTo("SYSTEM");
  }

  private OperationLog persistLog(String action, String details, String userEmail, LocalDateTime timestamp) {
    OperationLog log = new OperationLog();
    log.setAction(action);
    log.setDetails(details);
    log.setUserEmail(userEmail);
    log.setTimestamp(timestamp);
    return operationLogRepository.save(log);
  }
}
