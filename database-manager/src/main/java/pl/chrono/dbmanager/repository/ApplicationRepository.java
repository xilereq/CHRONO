package pl.chrono.dbmanager.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pl.chrono.dbmanager.model.Application;
import pl.chrono.dbmanager.model.enums.ApplicationStatus;

@Repository
public interface ApplicationRepository extends JpaRepository<Application, Long> {

  List<Application> findAllByEmployeeUserId(Integer employeeId);

  @Query("SELECT a FROM Application a WHERE a.status = :status AND a.employee.manager.userId = :managerId")
  List<Application> findPendingByManagerId(@Param("status") ApplicationStatus status, @Param("managerId") Integer managerId);
}
