package pl.chrono.dbmanager.repository;

import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import pl.chrono.dbmanager.model.WorkLog;

@Repository
public interface WorkLogRepository extends JpaRepository<WorkLog, Integer> {
  Optional<WorkLog> findByUser_UserIdAndEndTimeIsNullAndPendingCorrectionFalse(Integer userId);

  boolean existsByUser_UserIdAndEndTimeIsNullAndPendingCorrectionFalse(Integer userId);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  List<WorkLog> findByEndTimeIsNullAndPendingCorrectionFalseAndStartTimeBefore(LocalDateTime threshold);

  List<WorkLog> findByUser_UserIdAndStartTimeAfterAndPendingCorrectionFalse(Integer userId, LocalDateTime threshold);

  List<WorkLog> findByEndTimeIsNotNullAndSettledFalseAndPendingCorrectionFalse();

  List<WorkLog> findByPendingCorrectionTrueAndStartTimeBefore(LocalDateTime threshold);

  Optional<WorkLog> findTopByUser_UserIdAndEndTimeIsNotNullOrderByEndTimeDesc(Integer userId);

  List<WorkLog> findByUser_UserIdAndPendingCorrectionFalseAndStartTimeBetweenOrderByStartTimeAsc(
      Integer userId, LocalDateTime startOfDay, LocalDateTime endOfDay);

  @Query("SELECT w FROM WorkLog w WHERE w.user.userId = ?1 AND w.startTime BETWEEN ?2 AND ?3 AND w.pendingCorrection = false")
  Optional<WorkLog> findByUserUserIdAndStartTimeBetween(Integer userId, LocalDateTime startOfDay, LocalDateTime endOfDay);
}
