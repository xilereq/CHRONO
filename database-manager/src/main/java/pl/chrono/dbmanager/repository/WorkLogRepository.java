package pl.chrono.dbmanager.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pl.chrono.dbmanager.model.WorkLog;

@Repository
public interface WorkLogRepository extends JpaRepository<WorkLog, Integer> {}