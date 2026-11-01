package pl.chrono.usermanager.api;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import pl.chrono.usermanager.dto.request.CreateManagerDelegationRequest;
import pl.chrono.usermanager.dto.response.ManagerDelegationDto;

public interface I_ManagerDelegationService {

  ManagerDelegationDto createDelegation(CreateManagerDelegationRequest request, Integer requesterId);

  ManagerDelegationDto revokeDelegation(Long delegationId, Integer requesterId);

  List<ManagerDelegationDto> getDelegationsGivenBy(Integer managerId);

  List<ManagerDelegationDto> getDelegationsReceivedBy(Integer deputyId);

  List<ManagerDelegationDto> getAllDelegations();

  boolean isActiveDeputyOf(Integer deputyId, Integer managerId);

  boolean isActingDeputy(Integer userId);

  Set<Integer> getRepresentedManagerIds(Integer userId);

  Optional<Integer> findActiveDeputyId(Integer managerId);
}
