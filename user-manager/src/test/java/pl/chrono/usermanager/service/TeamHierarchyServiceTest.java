package pl.chrono.usermanager.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pl.chrono.dbmanager.model.Role;
import pl.chrono.dbmanager.model.User;
import pl.chrono.dbmanager.ports.I_UserData;
import pl.chrono.usermanager.api.I_ManagerDelegationService;
import pl.chrono.usermanager.dto.response.TeamTreeNodeDto;
import pl.chrono.usermanager.exception.TeamHierarchyException;

@ExtendWith(MockitoExtension.class)
class TeamHierarchyServiceTest {

  private static final Integer OUTSIDER_ID = 99;

  @Mock
  private I_UserData userData;
  @Mock
  private I_ManagerDelegationService delegationService;

  @InjectMocks
  private TeamHierarchyService service;

  // ceo(1) -> [alpha(2) -> [adam(4), bogdan(5)], beta(3) -> [celina(6)]]
  private User ceo;
  private User alpha;
  private User beta;
  private User adam;
  private User bogdan;
  private User celina;

  @BeforeEach
  void setUp() {
    ceo = user(1, "Zofia", "Prezes", null, "ROLE_MANAGER");
    alpha = user(2, "Anna", "Alfa", ceo, "ROLE_MANAGER");
    beta = user(3, "Bartek", "Beta", ceo, "ROLE_MANAGER");
    adam = user(4, "Adam", "Nowak", alpha, "ROLE_EMPLOYEE");
    bogdan = user(5, "Bogdan", "Kowal", alpha, "ROLE_EMPLOYEE");
    celina = user(6, "Celina", "Wrona", beta, "ROLE_EMPLOYEE");
  }

  @Test
  void buildsFullTreeWithCountsAndSortedChildren() {
    stubHierarchy(ceo, alpha, beta, adam, bogdan, celina);

    TeamTreeNodeDto tree = service.getTeamTree(1, null, null, false);

    assertThat(tree.userId()).isEqualTo(1);
    assertThat(tree.roles()).containsExactly("ROLE_MANAGER");
    assertThat(tree.directSubordinates()).isEqualTo(2);
    assertThat(tree.totalSubordinates()).isEqualTo(5);
    assertThat(tree.subordinates()).extracting(TeamTreeNodeDto::userId).containsExactly(2, 3);

    TeamTreeNodeDto alphaNode = tree.subordinates().get(0);
    assertThat(alphaNode.totalSubordinates()).isEqualTo(2);
    // sorted by surname: Kowal before Nowak
    assertThat(alphaNode.subordinates()).extracting(TeamTreeNodeDto::userId).containsExactly(5, 4);
    assertThat(alphaNode.subordinates().get(0).subordinates()).isEmpty();
  }

  @Test
  void loadsTreeWithOneBatchedQueryPerLevel() {
    stubHierarchy(ceo, alpha, beta, adam, bogdan, celina);

    service.getTeamTree(1, null, null, false);

    // levels: [1] -> [2,3] -> [4,5,6] -> nothing
    verify(userData, times(3)).getSubordinatesForManagers(any());
    verify(userData, never()).getSubordinatesForManager(anyInt());
  }

  @Test
  void depthLimitStopsLoadingDeeperLevels() {
    stubHierarchy(ceo, alpha, beta, adam, bogdan, celina);

    TeamTreeNodeDto tree = service.getTeamTree(1, null, 1, false);

    assertThat(tree.subordinates()).extracting(TeamTreeNodeDto::userId).containsExactly(2, 3);
    assertThat(tree.subordinates()).allSatisfy(node -> assertThat(node.subordinates()).isEmpty());
    verify(userData, times(1)).getSubordinatesForManagers(any());
  }

  @Test
  void inactiveManagerIsHiddenAndReportsAreLiftedToNearestVisibleSuperior() {
    alpha.setActive(false);
    stubHierarchy(ceo, alpha, beta, adam, bogdan, celina);

    TeamTreeNodeDto tree = service.getTeamTree(1, null, null, false);

    // Beta, Kowal, Nowak - sorted by surname after lifting
    assertThat(tree.subordinates()).extracting(TeamTreeNodeDto::userId).containsExactly(3, 5, 4);
    assertThat(tree.totalSubordinates()).isEqualTo(4);
  }

  @Test
  void includeInactiveKeepsInactiveUsersWithFlag() {
    alpha.setActive(false);
    stubHierarchy(ceo, alpha, beta, adam, bogdan, celina);

    TeamTreeNodeDto tree = service.getTeamTree(1, null, null, true);

    TeamTreeNodeDto alphaNode = tree.subordinates().get(0);
    assertThat(alphaNode.userId()).isEqualTo(2);
    assertThat(alphaNode.active()).isFalse();
    assertThat(alphaNode.directSubordinates()).isEqualTo(2);
    assertThat(tree.totalSubordinates()).isEqualTo(5);
  }

  @Test
  void cyclicManagerRelationDoesNotLoop() {
    User a = user(10, "A", "A", null, "ROLE_MANAGER");
    User b = user(11, "B", "B", a, "ROLE_MANAGER");
    a.setManager(b);
    stubHierarchy(a, b);

    TeamTreeNodeDto tree = service.getTeamTree(10, null, null, false);

    assertThat(tree.subordinates()).extracting(TeamTreeNodeDto::userId).containsExactly(11);
    assertThat(tree.subordinates().get(0).subordinates()).isEmpty();
    assertThat(tree.totalSubordinates()).isEqualTo(1);
  }

  @Test
  void superiorCanViewBranchBelow() {
    stubHierarchy(ceo, alpha, beta, adam, bogdan, celina);

    TeamTreeNodeDto tree = service.getTeamTree(1, 2, null, false);

    assertThat(tree.userId()).isEqualTo(2);
    assertThat(tree.totalSubordinates()).isEqualTo(2);
    verifyNoInteractions(delegationService);
  }

  @Test
  void deputyOfSuperiorCanViewBranch() {
    stubHierarchy(ceo, alpha, beta, adam, bogdan, celina);
    when(delegationService.getRepresentedManagerIds(OUTSIDER_ID)).thenReturn(Set.of(1));

    TeamTreeNodeDto tree = service.getTeamTree(OUTSIDER_ID, 3, null, false);

    assertThat(tree.userId()).isEqualTo(3);
    verify(userData, never()).findById(OUTSIDER_ID);
  }

  @Test
  void subordinateCannotViewSuperiorsTree() {
    when(userData.findById(1)).thenReturn(Optional.of(ceo));
    when(userData.findById(4)).thenReturn(Optional.of(adam));

    assertThatThrownBy(() -> service.getTeamTree(4, 1, null, false))
        .isInstanceOf(TeamHierarchyException.Forbidden.class);
    verify(userData, never()).getSubordinatesForManagers(any());
  }

  @Test
  void hrCanViewAnyTree() {
    stubHierarchy(ceo, alpha, beta, adam, bogdan, celina);
    when(userData.findById(OUTSIDER_ID))
        .thenReturn(Optional.of(user(OUTSIDER_ID, "H", "R", null, "ROLE_HR")));

    assertThat(service.getTeamTree(OUTSIDER_ID, 1, null, false).totalSubordinates()).isEqualTo(5);
  }

  @Test
  void unknownRootThrowsNotFound() {
    when(userData.findById(1234)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.getTeamTree(1, 1234, null, false))
        .isInstanceOf(TeamHierarchyException.UserNotFound.class);
  }

  @Test
  void rejectsNonPositiveManagerId() {
    assertThatThrownBy(() -> service.getTeamTree(1, 0, null, false))
        .isInstanceOf(TeamHierarchyException.InvalidManagerId.class);
    verifyNoInteractions(userData);
  }

  @Test
  void rejectsDepthBelowOne() {
    assertThatThrownBy(() -> service.getTeamTree(1, null, 0, false))
        .isInstanceOf(TeamHierarchyException.InvalidDepth.class);
    verifyNoInteractions(userData);
  }

  private void stubHierarchy(User... users) {
    List<User> all = List.of(users);
    for (User u : all) {
      lenient().when(userData.findById(u.getUserId())).thenReturn(Optional.of(u));
    }
    when(userData.getSubordinatesForManagers(any())).thenAnswer(invocation -> {
      Collection<Integer> managerIds = invocation.getArgument(0);
      return all.stream()
          .filter(u -> u.getManager() != null && managerIds.contains(u.getManager().getUserId()))
          .toList();
    });
  }

  private static User user(int id, String name, String surname, User manager, String roleName) {
    User user = new User();
    user.setUserId(id);
    user.setName(name);
    user.setSurname(surname);
    user.setEmail(name.toLowerCase() + id + "@test.pl");
    user.setManager(manager);
    Role role = new Role();
    role.setName(roleName);
    user.setRoles(Set.of(role));
    return user;
  }
}
