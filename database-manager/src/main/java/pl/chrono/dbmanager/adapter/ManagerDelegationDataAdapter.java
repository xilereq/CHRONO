package pl.chrono.dbmanager.adapter;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import pl.chrono.dbmanager.model.ManagerDelegation;
import pl.chrono.dbmanager.ports.I_ManagerDelegationData;
import pl.chrono.dbmanager.repository.ManagerDelegationRepository;

@Component
@RequiredArgsConstructor
public class ManagerDelegationDataAdapter implements I_ManagerDelegationData {

  private final ManagerDelegationRepository repository;

  @Override
  public ManagerDelegation save(ManagerDelegation delegation) {
    return repository.save(delegation);
  }

  @Override
  public Optional<ManagerDelegation> findById(Long delegationId) {
    return repository.findById(delegationId);
  }

  @Override
  public List<ManagerDelegation> findActiveByDeputy(Integer deputyId, LocalDate date) {
    return repository.findActiveByDeputy(deputyId, date);
  }

  @Override
  public List<ManagerDelegation> findActiveByManager(Integer managerId, LocalDate date) {
    return repository.findActiveByManager(managerId, date);
  }

  @Override
  public boolean isActiveDeputyOf(Integer deputyId, Integer managerId, LocalDate date) {
    return repository.isActiveDeputyOf(deputyId, managerId, date);
  }

  @Override
  public boolean existsOverlapping(Integer managerId, LocalDate startDate, LocalDate endDate) {
    return repository.existsOverlapping(managerId, startDate, endDate);
  }

  @Override
  public List<ManagerDelegation> findAllByManager(Integer managerId) {
    return repository.findAllByManagerUserIdOrderByStartDateDesc(managerId);
  }

  @Override
  public List<ManagerDelegation> findAllByDeputy(Integer deputyId) {
    return repository.findAllByDeputyUserIdOrderByStartDateDesc(deputyId);
  }

  @Override
  public List<ManagerDelegation> findAll() {
    return repository.findAllByOrderByStartDateDesc();
  }
}
