package pl.chrono.dbmanager.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pl.chrono.dbmanager.model.OvertimeArchive;
import java.util.List;

public interface OvertimeArchiveRepository extends JpaRepository<OvertimeArchive, Long> {

  @Query("""
        SELECT COALESCE(
            SUM(CASE\s
                WHEN o.type IN ('EARNED', 'MANUAL_ADDITION', 'REFUNDED') THEN o.durationSeconds\s
                WHEN o.type IN ('LOCKED', 'EXPIRED', 'MANUAL_DEDUCTION') THEN -o.durationSeconds\s
                ELSE 0\s
            END), 0
        )\s
        FROM OvertimeArchive o\s
        WHERE o.user.userId = :userId
   \s""")
  Integer calculateOvertimeBalanceSeconds(@Param("userId") Integer userId);

  List<OvertimeArchive> findByUser_UserId(Integer userId);
}
