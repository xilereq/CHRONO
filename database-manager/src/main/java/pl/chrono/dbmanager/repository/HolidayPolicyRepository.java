package pl.chrono.dbmanager.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import pl.chrono.dbmanager.model.Holiday;
import pl.chrono.dbmanager.model.HolidayPolicy;
import pl.chrono.dbmanager.model.Policy;

@Repository
public interface HolidayPolicyRepository extends JpaRepository<HolidayPolicy, Integer> {
  void deleteByHoliday(Holiday holiday);

  void deleteByPolicy(Policy policy);

  List<HolidayPolicy> findByPolicy(Policy policy);

  Optional<HolidayPolicy> findByPolicyAndHoliday(Policy policy, Holiday holiday);

  boolean existsByPolicyAndHoliday(Policy policy, Holiday holiday);

  @Modifying
  @Query("DELETE FROM HolidayPolicy hp WHERE hp.holiday.holidayId = :holidayId")
  void deleteByHolidayId(@Param("holidayId") Integer holidayId);

}
