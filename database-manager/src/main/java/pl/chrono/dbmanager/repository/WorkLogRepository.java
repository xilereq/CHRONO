package pl.chrono.dbmanager.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pl.chrono.dbmanager.model.WorkLog;

@Repository
public interface WorkLogRepository extends JpaRepository<WorkLog, Integer> {
  Optional<WorkLog> findByUser_UserIdAndEndTimeIsNull(Integer userId);

  boolean existsByUser_UserIdAndEndTimeIsNull(Integer userId);

  List<WorkLog> findByEndTimeIsNullAndStartTimeBefore(LocalDateTime threshold);
}
