package pl.chrono.dbmanager.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pl.chrono.dbmanager.model.LeaveLog;

@Repository
public interface LeaveLogRepository extends JpaRepository<LeaveLog, Integer> {}