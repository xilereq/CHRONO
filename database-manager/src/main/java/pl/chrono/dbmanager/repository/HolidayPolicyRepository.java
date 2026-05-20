package pl.chrono.dbmanager.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pl.chrono.dbmanager.model.Holiday;
import pl.chrono.dbmanager.model.HolidayPolicy;
import pl.chrono.dbmanager.model.Policy;
import java.util.List;
import java.util.Optional;

@Repository
public interface HolidayPolicyRepository extends JpaRepository<HolidayPolicy, Integer> {
  void deleteByHoliday(Holiday holiday);

  void deleteByPolicy(Policy policy);

  List<HolidayPolicy> findByPolicy(Policy policy);

  Optional<HolidayPolicy> findByPolicyAndHoliday(Policy policy, Holiday holiday);

  boolean existsByPolicyAndHoliday(Policy policy, Holiday holiday);
}
