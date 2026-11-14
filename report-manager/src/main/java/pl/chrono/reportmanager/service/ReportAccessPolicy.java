package pl.chrono.reportmanager.service;

import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.chrono.dbmanager.model.User;
import pl.chrono.dbmanager.repository.UserRepository;
import pl.chrono.reportmanager.api.I_ReportAccessPolicy;
import pl.chrono.reportmanager.exception.ReportUserException;
import pl.chrono.usermanager.api.I_ManagerDelegationService;

@Service
@RequiredArgsConstructor
public class ReportAccessPolicy implements I_ReportAccessPolicy {

  private static final Set<String> FULL_ACCESS_ROLES = Set.of("ROLE_HR", "ROLE_ADMIN");

  private final UserRepository userRepository;
  private final I_ManagerDelegationService delegationService;

  @Override
  @Transactional(readOnly = true)
  public void checkCanViewEmployee(Integer actorId, Integer employeeId) {
    if (!canViewEmployee(actorId, employeeId)) {
      throw new ReportUserException.AccessDenied(employeeId);
    }
  }

  private boolean canViewEmployee(Integer actorId, Integer employeeId) {
    if (actorId.equals(employeeId) || hasFullAccess(findUser(actorId))) {
      return true;
    }
    User manager = findUser(employeeId).getManager();
    return manager != null
        && (manager.getUserId().equals(actorId)
        || delegationService.isActiveDeputyOf(actorId, manager.getUserId()));
  }

  private boolean hasFullAccess(User user) {
    return user.getAuthorities().stream()
        .anyMatch(authority -> FULL_ACCESS_ROLES.contains(authority.getAuthority()));
  }

  private User findUser(Integer userId) {
    return userRepository.findById(userId)
        .orElseThrow(() -> new ReportUserException.NotFound(userId));
  }
}
