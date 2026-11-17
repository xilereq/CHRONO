package pl.chrono.usermanager.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class TeamHierarchyException extends RuntimeException {
  private final String errorCode;
  private final HttpStatus status;

  public TeamHierarchyException(String errorCode, String message, HttpStatus status) {
    super(message);
    this.errorCode = errorCode;
    this.status = status;
  }

  public static class UserNotFound extends TeamHierarchyException {
    public UserNotFound(Integer userId) {
      super("USER_NOT_FOUND", "User with ID " + userId + " does not exist.", HttpStatus.NOT_FOUND);
    }
  }

  public static class Forbidden extends TeamHierarchyException {
    public Forbidden(Integer managerId) {
      super("TEAM_TREE_FORBIDDEN",
          "Access Denied. You cannot view the team tree of user " + managerId + ".",
          HttpStatus.FORBIDDEN);
    }
  }

  public static class InvalidManagerId extends TeamHierarchyException {
    public InvalidManagerId(Integer managerId) {
      super("INVALID_MANAGER_ID",
          "Manager ID must be a positive number, got: " + managerId + ".", HttpStatus.BAD_REQUEST);
    }
  }

  public static class InvalidDepth extends TeamHierarchyException {
    public InvalidDepth(Integer depth) {
      super("INVALID_TREE_DEPTH",
          "Tree depth must be at least 1, got: " + depth + ".", HttpStatus.BAD_REQUEST);
    }
  }
}
