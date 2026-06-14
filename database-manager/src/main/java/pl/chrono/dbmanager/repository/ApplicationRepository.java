package pl.chrono.dbmanager.repository;

import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import pl.chrono.dbmanager.model.Application;
import pl.chrono.dbmanager.model.enums.ApplicationStatus;
import pl.chrono.dbmanager.model.enums.ApplicationType;

@Repository
public interface ApplicationRepository extends JpaRepository<Application, Long> {

  List<Application> findAllByEmployeeUserId(Integer employeeId);

  @Query("SELECT a FROM Application a WHERE a.status = :status AND a.employee.manager.userId = :managerId")
  List<Application> findPendingByManagerId(@Param("status") ApplicationStatus status, @Param("managerId") Integer managerId);

  @Query("SELECT a FROM Application a WHERE a.employee.userId = :employeeId AND a.status = 'APPROVED' AND :date BETWEEN a.startDate AND a.endDate")
  List<Application> findApprovedByEmployeeAndDate(@Param("employeeId") Integer employeeId, @Param("date") LocalDate date);

  @Query("SELECT a FROM Application a WHERE a.employee.manager.userId = :managerId")
  List<Application> findAllByManagerId(@Param("managerId") Integer managerId);

  @Query("SELECT a FROM Application a WHERE a.employee.userId = :employeeId AND a.type = :type AND a.status = :status AND a.startDate >= :startOfYear AND a.endDate <= :endOfYear")
  List<Application> findApplicationsByEmployeeAndTypeAndDateRange(
      @Param("employeeId") Integer employeeId,
      @Param("type") ApplicationType type,
      @Param("status") ApplicationStatus status,
      @Param("startOfYear") LocalDate startOfYear,
      @Param("endOfYear") LocalDate endOfYear
  );

  List<Application> findByEmployeeManagerUserIdAndStartDateBetween(Integer managerId, LocalDate startDate, LocalDate endDate);
}
