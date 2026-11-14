package pl.chrono.reportmanager.service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pl.chrono.dbmanager.model.Role;
import pl.chrono.dbmanager.model.User;
import pl.chrono.dbmanager.repository.UserRepository;
import pl.chrono.reportmanager.exception.ReportUserException;
import pl.chrono.usermanager.api.I_ManagerDelegationService;

@ExtendWith(MockitoExtension.class)
class ReportAccessPolicyTest {

  private static final int MANAGER_ID = 10;
  private static final int OTHER_MANAGER_ID = 20;
  private static final int EMPLOYEE_ID = 30;

  @Mock private UserRepository userRepository;
  @Mock private I_ManagerDelegationService delegationService;

  @InjectMocks private ReportAccessPolicy accessPolicy;

  @Test
  void employeeCanViewOwnReport() {
    assertThatCode(() -> accessPolicy.checkCanViewEmployee(EMPLOYEE_ID, EMPLOYEE_ID))
        .doesNotThrowAnyException();
  }

  @Test
  void hrCanViewAnyEmployee() {
    givenUser(user(OTHER_MANAGER_ID, "ROLE_HR", null));

    assertThatCode(() -> accessPolicy.checkCanViewEmployee(OTHER_MANAGER_ID, EMPLOYEE_ID))
        .doesNotThrowAnyException();
  }

  @Test
  void adminCanViewAnyEmployee() {
    givenUser(user(OTHER_MANAGER_ID, "ROLE_ADMIN", null));

    assertThatCode(() -> accessPolicy.checkCanViewEmployee(OTHER_MANAGER_ID, EMPLOYEE_ID))
        .doesNotThrowAnyException();
  }

  @Test
  void managerCanViewOwnSubordinate() {
    User manager = user(MANAGER_ID, "ROLE_MANAGER", null);
    givenUser(manager);
    givenUser(user(EMPLOYEE_ID, "ROLE_EMPLOYEE", manager));

    assertThatCode(() -> accessPolicy.checkCanViewEmployee(MANAGER_ID, EMPLOYEE_ID))
        .doesNotThrowAnyException();
  }

  @Test
  void activeDeputyCanViewSubordinateOfRepresentedManager() {
    givenUser(user(OTHER_MANAGER_ID, "ROLE_MANAGER", null));
    givenUser(user(EMPLOYEE_ID, "ROLE_EMPLOYEE", user(MANAGER_ID, "ROLE_MANAGER", null)));
    when(delegationService.isActiveDeputyOf(OTHER_MANAGER_ID, MANAGER_ID)).thenReturn(true);

    assertThatCode(() -> accessPolicy.checkCanViewEmployee(OTHER_MANAGER_ID, EMPLOYEE_ID))
        .doesNotThrowAnyException();
  }

  @Test
  void managerCannotViewEmployeeFromAnotherTeam() {
    givenUser(user(OTHER_MANAGER_ID, "ROLE_MANAGER", null));
    givenUser(user(EMPLOYEE_ID, "ROLE_EMPLOYEE", user(MANAGER_ID, "ROLE_MANAGER", null)));
    when(delegationService.isActiveDeputyOf(OTHER_MANAGER_ID, MANAGER_ID)).thenReturn(false);

    assertThatThrownBy(() -> accessPolicy.checkCanViewEmployee(OTHER_MANAGER_ID, EMPLOYEE_ID))
        .isInstanceOf(ReportUserException.AccessDenied.class);
  }

  @Test
  void managerCannotViewEmployeeWithoutManager() {
    givenUser(user(MANAGER_ID, "ROLE_MANAGER", null));
    givenUser(user(EMPLOYEE_ID, "ROLE_EMPLOYEE", null));

    assertThatThrownBy(() -> accessPolicy.checkCanViewEmployee(MANAGER_ID, EMPLOYEE_ID))
        .isInstanceOf(ReportUserException.AccessDenied.class);
  }

  @Test
  void unknownEmployeeIsReportedAsNotFound() {
    givenUser(user(MANAGER_ID, "ROLE_MANAGER", null));
    when(userRepository.findById(EMPLOYEE_ID)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> accessPolicy.checkCanViewEmployee(MANAGER_ID, EMPLOYEE_ID))
        .isInstanceOf(ReportUserException.NotFound.class);
  }

  private void givenUser(User user) {
    when(userRepository.findById(user.getUserId())).thenReturn(Optional.of(user));
  }

  private User user(int id, String roleName, User manager) {
    Role role = new Role();
    role.setName(roleName);
    User user = new User();
    user.setUserId(id);
    user.setRoles(Set.of(role));
    user.setManager(manager);
    return user;
  }
}
