package pl.chrono.dbmanager.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.stereotype.Repository;
import pl.chrono.dbmanager.model.WorkLog;

@Repository
public interface WorkLogRepository extends JpaRepository<WorkLog, Integer> {
  Optional<WorkLog> findByUser_UserIdAndEndTimeIsNull(Integer userId);

  boolean existsByUser_UserIdAndEndTimeIsNull(Integer userId);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  List<WorkLog> findByEndTimeIsNullAndStartTimeBefore(LocalDateTime threshold);

  List<WorkLog> findByUser_UserIdAndStartTimeAfter(Integer userId, LocalDateTime threshold);

  List<WorkLog> findByEndTimeIsNotNullAndSettledFalse();

  Optional<WorkLog> findByUserUserIdAndStartTimeBetween(Integer employeeId,
                                                        LocalDateTime startOfDay,
                                                        LocalDateTime endOfDay);

  Optional<WorkLog> findTopByUser_UserIdAndEndTimeIsNotNullOrderByEndTimeDesc(Integer userId);

  List<WorkLog> findByUser_UserIdAndStartTimeBetweenOrderByStartTimeAsc(Integer userId,
                                                                        LocalDateTime startOfDay,
                                                                        LocalDateTime endOfDay);
}
