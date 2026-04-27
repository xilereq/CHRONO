package pl.chrono.dbmanager.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pl.chrono.dbmanager.model.Application;
import java.util.List;

@Repository
public interface ApplicationRepository extends JpaRepository<Application, Long> {

  List<Application> findAllByEmployeeUserId(Integer employeeId);
}
