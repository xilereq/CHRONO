package pl.chrono.dbmanager.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import pl.chrono.dbmanager.model.OperationLog;
import java.time.LocalDateTime;

@Repository
public interface OperationLogRepository extends JpaRepository<OperationLog, Long> {
  @Modifying
  @Query("DELETE FROM OperationLog o WHERE o.timestamp < :cutoffDate")
  int deleteOlderThan(@Param("cutoffDate") LocalDateTime cutoffDate);

  boolean existsByActionAndTimestampBetween(String action, LocalDateTime start, LocalDateTime end);
}
