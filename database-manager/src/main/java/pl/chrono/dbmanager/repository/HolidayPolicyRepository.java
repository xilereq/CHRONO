package pl.chrono.dbmanager.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pl.chrono.dbmanager.model.HolidayPolicy;

@Repository
public interface HolidayPolicyRepository extends JpaRepository<HolidayPolicy, Integer> {}