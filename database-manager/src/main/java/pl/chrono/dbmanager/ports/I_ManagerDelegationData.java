package pl.chrono.dbmanager.ports;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import pl.chrono.dbmanager.model.ManagerDelegation;

public interface I_ManagerDelegationData {

  ManagerDelegation save(ManagerDelegation delegation);

  Optional<ManagerDelegation> findById(Long delegationId);

  List<ManagerDelegation> findActiveByDeputy(Integer deputyId, LocalDate date);

  List<ManagerDelegation> findActiveByManager(Integer managerId, LocalDate date);

  boolean isActiveDeputyOf(Integer deputyId, Integer managerId, LocalDate date);

  boolean existsOverlapping(Integer managerId, LocalDate startDate, LocalDate endDate);

  List<ManagerDelegation> findAllByManager(Integer managerId);

  List<ManagerDelegation> findAllByDeputy(Integer deputyId);

  List<ManagerDelegation> findAll();
}
