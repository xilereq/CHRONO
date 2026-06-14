package pl.chrono.dbmanager.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import pl.chrono.dbmanager.model.Holiday;
import java.time.LocalDate;
import java.util.Set;

@Repository
public interface HolidayRepository extends JpaRepository<Holiday, Integer> {
  @Query("SELECT COUNT(h) > 0 FROM Holiday h WHERE h.date = :date OR (h.occursYearly = true AND " +
      "MONTH(h.date) = MONTH(:date) AND DAY(h.date) = DAY(:date))")
  boolean existsByDateOrYearly(@Param("date") LocalDate date);

  @Query("SELECT h.date FROM Holiday h WHERE h.date BETWEEN :start AND :end OR h.occursYearly = true")
  Set<LocalDate> findHolidayDatesBetween(@Param("start") LocalDate start, @Param("end") LocalDate end);

  @Modifying
  @Query("DELETE FROM Holiday h WHERE h.holidayId = :holidayId")
  void deleteByIdDirectly(@Param("holidayId") Integer holidayId);
}
