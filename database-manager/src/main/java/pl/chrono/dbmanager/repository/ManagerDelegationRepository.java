package pl.chrono.dbmanager.repository;

import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pl.chrono.dbmanager.model.ManagerDelegation;

public interface ManagerDelegationRepository extends JpaRepository<ManagerDelegation, Long> {

  @Query("SELECT d FROM ManagerDelegation d WHERE d.deputy.userId = :deputyId "
      + "AND d.revokedAt IS NULL AND :date BETWEEN d.startDate AND d.endDate")
  List<ManagerDelegation> findActiveByDeputy(@Param("deputyId") Integer deputyId,
                                             @Param("date") LocalDate date);

  @Query("SELECT d FROM ManagerDelegation d WHERE d.manager.userId = :managerId "
      + "AND d.revokedAt IS NULL AND :date BETWEEN d.startDate AND d.endDate")
  List<ManagerDelegation> findActiveByManager(@Param("managerId") Integer managerId,
                                              @Param("date") LocalDate date);

  @Query("SELECT CASE WHEN COUNT(d) > 0 THEN true ELSE false END FROM ManagerDelegation d "
      + "WHERE d.deputy.userId = :deputyId AND d.manager.userId = :managerId "
      + "AND d.revokedAt IS NULL AND :date BETWEEN d.startDate AND d.endDate")
  boolean isActiveDeputyOf(@Param("deputyId") Integer deputyId,
                           @Param("managerId") Integer managerId,
                           @Param("date") LocalDate date);

  @Query("SELECT CASE WHEN COUNT(d) > 0 THEN true ELSE false END FROM ManagerDelegation d "
      + "WHERE d.manager.userId = :managerId AND d.revokedAt IS NULL "
      + "AND d.startDate <= :endDate AND d.endDate >= :startDate")
  boolean existsOverlapping(@Param("managerId") Integer managerId,
                            @Param("startDate") LocalDate startDate,
                            @Param("endDate") LocalDate endDate);

  List<ManagerDelegation> findAllByManagerUserIdOrderByStartDateDesc(Integer managerId);

  List<ManagerDelegation> findAllByDeputyUserIdOrderByStartDateDesc(Integer deputyId);

  List<ManagerDelegation> findAllByOrderByStartDateDesc();
}
