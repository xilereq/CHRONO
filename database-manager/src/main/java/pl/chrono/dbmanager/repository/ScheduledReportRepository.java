package pl.chrono.dbmanager.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import pl.chrono.dbmanager.model.ScheduledReport;

@Repository
public interface ScheduledReportRepository extends JpaRepository<ScheduledReport, Long> {

  @Query("""
      SELECT DISTINCT sr FROM ScheduledReport sr
      JOIN FETCH sr.creator
      LEFT JOIN FETCH sr.targetUsers
      WHERE sr.isActive = true AND sr.nextRunAt <= :now
      """)
  List<ScheduledReport> findAllDueWithDetails(@Param("now") LocalDateTime now);

  @Query("""
      SELECT DISTINCT sr FROM ScheduledReport sr
      JOIN FETCH sr.creator c
      LEFT JOIN FETCH sr.targetUsers
      WHERE c.userId = :creatorId
      ORDER BY sr.createdAt DESC
      """)
  List<ScheduledReport> findAllByCreatorIdWithDetails(@Param("creatorId") Integer creatorId);

  @Query("""
      SELECT DISTINCT sr FROM ScheduledReport sr
      JOIN FETCH sr.creator
      LEFT JOIN FETCH sr.targetUsers
      ORDER BY sr.createdAt DESC
      """)
  List<ScheduledReport> findAllWithDetails();

  @Query("""
      SELECT sr FROM ScheduledReport sr
      JOIN FETCH sr.creator
      LEFT JOIN FETCH sr.targetUsers
      WHERE sr.scheduleId = :scheduleId
      """)
  Optional<ScheduledReport> findByIdWithDetails(@Param("scheduleId") Long scheduleId);
}
