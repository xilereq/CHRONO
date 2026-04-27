package pl.chrono.dbmanager.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pl.chrono.dbmanager.model.OperationLog;

@Repository
public interface OperationLogRepository extends JpaRepository<OperationLog, Long> {
}
