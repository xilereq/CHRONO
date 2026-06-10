package pl.chrono.dbmanager.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pl.chrono.dbmanager.model.OvertimeArchive;
import pl.chrono.dbmanager.model.enums.OvertimeType;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface OvertimeArchiveRepository extends JpaRepository<OvertimeArchive, Long> {

  @Query("""
        SELECT COALESCE(
            SUM(CASE\s
                WHEN o.type IN ('EARNED', 'MANUAL_ADDITION', 'REFUNDED') THEN o.durationSeconds\s
                WHEN o.type IN ('LOCKED', 'EXPIRED',\s
                   'MANUAL_SUBTRACTION', 'DEBT', 'USED') THEN -o.durationSeconds\s
                ELSE 0\s
            END), 0
        )\s
        FROM OvertimeArchive o\s
        WHERE o.user.userId = :userId
   \s""")
  Integer calculateOvertimeBalanceSeconds(@Param("userId") Integer userId);

  @Query("""
        SELECT COALESCE(SUM(o.durationSeconds), 0)
        FROM OvertimeArchive o
        WHERE o.user.userId = :userId
          AND o.type = 'EARNED'
          AND o.workLog.startTime BETWEEN :startOfYear AND :endOfYear
   """)
  Integer sumEarnedOvertimeBetween(
      @Param("userId") Integer userId,
      @Param("startOfYear") LocalDateTime startOfYear,
      @Param("endOfYear") LocalDateTime endOfYear
  );

  List<OvertimeArchive> findByUser_UserId(Integer userId);

  Optional<OvertimeArchive> findByApplication_IdAndType(Long applicationId, OvertimeType type);
}
