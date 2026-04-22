package pl.chrono.dbmanager.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pl.chrono.dbmanager.model.Holiday;

@Repository
public interface HolidayRepository extends JpaRepository<Holiday, Integer> {
}
