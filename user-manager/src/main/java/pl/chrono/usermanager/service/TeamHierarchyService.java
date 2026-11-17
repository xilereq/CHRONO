package pl.chrono.usermanager.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.chrono.dbmanager.model.Role;
import pl.chrono.dbmanager.model.User;
import pl.chrono.dbmanager.ports.I_UserData;
import pl.chrono.usermanager.api.I_ManagerDelegationService;
import pl.chrono.usermanager.api.I_TeamHierarchyService;
import pl.chrono.usermanager.dto.response.TeamTreeNodeDto;
import pl.chrono.usermanager.exception.TeamHierarchyException;

@Service
@RequiredArgsConstructor
public class TeamHierarchyService implements I_TeamHierarchyService {

  private static final Set<String> PRIVILEGED_ROLES = Set.of("ROLE_ADMIN", "ROLE_HR");
  private static final Comparator<TeamTreeNodeDto> BY_SURNAME_AND_NAME = Comparator
      .comparing(TeamTreeNodeDto::surname, String.CASE_INSENSITIVE_ORDER)
      .thenComparing(TeamTreeNodeDto::name, String.CASE_INSENSITIVE_ORDER);

  private final I_UserData userData;
  private final I_ManagerDelegationService delegationService;

  @Transactional(readOnly = true)
  @Override
  public TeamTreeNodeDto getTeamTree(Integer requesterId, Integer managerId, Integer maxDepth,
                                     boolean includeInactive) {
    if (managerId != null && managerId <= 0) {
      throw new TeamHierarchyException.InvalidManagerId(managerId);
    }
    if (maxDepth != null && maxDepth < 1) {
      throw new TeamHierarchyException.InvalidDepth(maxDepth);
    }
    Integer rootId = managerId != null ? managerId : requesterId;

    User root = userData.findById(rootId)
        .orElseThrow(() -> new TeamHierarchyException.UserNotFound(rootId));
    validateAccess(requesterId, root);

    Map<Integer, List<User>> childrenByManager =
        loadSubordinatesLevelByLevel(rootId, maxDepth != null ? maxDepth : Integer.MAX_VALUE);

    return toNode(root, childrenByManager, includeInactive, LocalDate.now());
  }

  /**
   * Access is granted to the root itself, anyone above the root in the chain of command,
   * an active deputy of the root or of anyone above it, and HR/Admin.
   */
  private void validateAccess(Integer requesterId, User root) {
    if (requesterId.equals(root.getUserId())) {
      return;
    }
    Set<Integer> chainOfCommand = superiorsOf(root);
    if (chainOfCommand.contains(requesterId)) {
      return;
    }
    Set<Integer> represented = new HashSet<>(delegationService.getRepresentedManagerIds(requesterId));
    represented.remove(requesterId);
    if (represented.contains(root.getUserId())
        || represented.stream().anyMatch(chainOfCommand::contains)) {
      return;
    }
    User requester = userData.findById(requesterId)
        .orElseThrow(() -> new TeamHierarchyException.UserNotFound(requesterId));
    boolean privileged = requester.getRoles().stream()
        .map(Role::getName)
        .anyMatch(PRIVILEGED_ROLES::contains);
    if (!privileged) {
      throw new TeamHierarchyException.Forbidden(root.getUserId());
    }
  }

  private Set<Integer> superiorsOf(User user) {
    Set<Integer> superiors = new HashSet<>();
    User current = user.getManager();
    // The visited check also guards against cycles in the manager relation.
    while (current != null && !current.getUserId().equals(user.getUserId())
        && superiors.add(current.getUserId())) {
      current = current.getManager();
    }
    return superiors;
  }

  /**
   * Breadth-first load: one batched query per hierarchy level instead of one per person.
   * Users already placed in the tree are skipped, so a cyclic manager relation cannot loop.
   */
  private Map<Integer, List<User>> loadSubordinatesLevelByLevel(Integer rootId, int maxDepth) {
    Map<Integer, List<User>> childrenByManager = new HashMap<>();
    Set<Integer> visited = new HashSet<>(Set.of(rootId));
    List<Integer> frontier = List.of(rootId);

    for (int depth = 0; depth < maxDepth && !frontier.isEmpty(); depth++) {
      List<Integer> nextFrontier = new ArrayList<>();
      for (User child : userData.getSubordinatesForManagers(frontier)) {
        if (!visited.add(child.getUserId())) {
          continue;
        }
        childrenByManager.computeIfAbsent(child.getManager().getUserId(), id -> new ArrayList<>())
            .add(child);
        nextFrontier.add(child.getUserId());
      }
      frontier = nextFrontier;
    }
    return childrenByManager;
  }

  private TeamTreeNodeDto toNode(User user, Map<Integer, List<User>> childrenByManager,
                                 boolean includeInactive, LocalDate today) {
    List<TeamTreeNodeDto> subordinates =
        visibleSubordinates(user.getUserId(), childrenByManager, includeInactive, today);
    int total = subordinates.size()
        + subordinates.stream().mapToInt(TeamTreeNodeDto::totalSubordinates).sum();
    List<String> roles = user.getRoles().stream().map(Role::getName).sorted().toList();

    return new TeamTreeNodeDto(user.getUserId(), user.getName(), user.getSurname(), user.getEmail(),
        roles, user.isEmployedOn(today), subordinates.size(), total, subordinates);
  }

  private List<TeamTreeNodeDto> visibleSubordinates(Integer managerId,
                                                    Map<Integer, List<User>> childrenByManager,
                                                    boolean includeInactive, LocalDate today) {
    List<TeamTreeNodeDto> result = new ArrayList<>();
    for (User child : childrenByManager.getOrDefault(managerId, List.of())) {
      if (includeInactive || child.isEmployedOn(today)) {
        result.add(toNode(child, childrenByManager, includeInactive, today));
      } else {
        // Hidden (inactive) user: attach their reports to the nearest visible superior.
        result.addAll(visibleSubordinates(child.getUserId(), childrenByManager, false, today));
      }
    }
    result.sort(BY_SURNAME_AND_NAME);
    return result;
  }
}
