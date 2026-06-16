package pl.chrono.dbmanager.repository;

import java.time.LocalDateTime;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import pl.chrono.dbmanager.model.OperationLog;

@DataJpaTest
class OperationLogRepositoryIT {

  @Autowired
  private TestEntityManager em;

  @Autowired
  private OperationLogRepository repository;

  @Test
  void cutoffDateTest() {
    LocalDateTime now = LocalDateTime.now();
    LocalDateTime cutoffDate = now.minusMonths(3);

    persistLog(now.minusMonths(6));
    persistLog(now.minusMonths(4));

    OperationLog recentLog1 = persistLog(now.minusMonths(2));
    OperationLog recentLog2 = persistLog(now.minusMonths(1));
    em.flush();

    int deletedCount = repository.deleteOlderThan(cutoffDate);

    assertThat(deletedCount).isEqualTo(2);

    List<OperationLog> remainingLogs = repository.findAll();
    assertThat(remainingLogs).hasSize(2).extracting(OperationLog::getId)
        .containsExactlyInAnyOrder(recentLog1.getId(), recentLog2.getId());
  }

  @Test
  void noOldLogsToDeleteTest() {
    LocalDateTime now = LocalDateTime.now();
    persistLog(now.minusMonths(1));
    em.flush();

    LocalDateTime cutoffDate = now.minusMonths(3);

    int deletedCount = repository.deleteOlderThan(cutoffDate);

    assertThat(deletedCount).isZero();
    assertThat(repository.findAll()).hasSize(1);
  }

  private OperationLog persistLog(LocalDateTime timestamp) {
    OperationLog log = new OperationLog();
    log.setAction("TEST_ACTION");
    log.setUserEmail("test@test.pl");
    log.setTimestamp(timestamp);
    return em.persist(log);
  }
}
