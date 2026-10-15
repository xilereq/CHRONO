package pl.chrono.dbmanager.repository;

import java.time.LocalDate;
import java.util.Optional;
import java.util.Set;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import pl.chrono.dbmanager.model.Holiday;

@Repository
public interface HolidayRepository extends JpaRepository<Holiday, Integer> {
  boolean existsByDate(LocalDate date);

  boolean existsByNameIgnoreCaseAndDate(String name, LocalDate date);

  Optional<Holiday> findByNameIgnoreCaseAndDate(String name, LocalDate date);

  @Query("SELECT h.date FROM Holiday h WHERE h.date BETWEEN :start AND :end")
  Set<LocalDate> findHolidayDatesBetween(@Param("start") LocalDate start, @Param("end") LocalDate end);

  @Modifying
  @Query("DELETE FROM Holiday h WHERE h.holidayId = :holidayId")
  void deleteByIdDirectly(@Param("holidayId") Integer holidayId);


}
