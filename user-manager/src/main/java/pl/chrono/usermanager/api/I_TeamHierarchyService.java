package pl.chrono.usermanager.api;

import pl.chrono.usermanager.dto.response.TeamTreeNodeDto;

public interface I_TeamHierarchyService {

  /**
   * Builds the subordination tree rooted at the given manager.
   *
   * @param requesterId     the authenticated user
   * @param managerId       root of the tree; {@code null} means the requester
   * @param maxDepth        how many levels below the root to load; {@code null} means all
   * @param includeInactive whether users not employed today are shown; when {@code false}
   *                        their subordinates are attached to the nearest visible superior
   */
  TeamTreeNodeDto getTeamTree(Integer requesterId, Integer managerId, Integer maxDepth,
                              boolean includeInactive);
}
