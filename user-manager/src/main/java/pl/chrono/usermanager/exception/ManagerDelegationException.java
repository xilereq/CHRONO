package pl.chrono.usermanager.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class ManagerDelegationException extends RuntimeException {
  private final String errorCode;
  private final HttpStatus status;

  public ManagerDelegationException(String errorCode, String message, HttpStatus status) {
    super(message);
    this.errorCode = errorCode;
    this.status = status;
  }

  public static class NotFound extends ManagerDelegationException {
    public NotFound(Long delegationId) {
      super("DELEGATION_NOT_FOUND",
          "Delegation with ID " + delegationId + " does not exist.", HttpStatus.NOT_FOUND);
    }
  }

  public static class UserNotFound extends ManagerDelegationException {
    public UserNotFound(Integer userId) {
      super("USER_NOT_FOUND", "User with ID " + userId + " does not exist.", HttpStatus.NOT_FOUND);
    }
  }

  public static class Forbidden extends ManagerDelegationException {
    public Forbidden(String message) {
      super("DELEGATION_FORBIDDEN", message, HttpStatus.FORBIDDEN);
    }
  }

  public static class NotAManager extends ManagerDelegationException {
    public NotAManager(Integer userId) {
      super("DELEGATION_NOT_A_MANAGER",
          "User with ID " + userId + " does not have the MANAGER role.", HttpStatus.BAD_REQUEST);
    }
  }

  public static class InvalidDeputy extends ManagerDelegationException {
    public InvalidDeputy(String message) {
      super("DELEGATION_INVALID_DEPUTY", message, HttpStatus.BAD_REQUEST);
    }
  }

  public static class InvalidPeriod extends ManagerDelegationException {
    public InvalidPeriod(String message) {
      super("DELEGATION_INVALID_PERIOD", message, HttpStatus.BAD_REQUEST);
    }
  }

  public static class Overlapping extends ManagerDelegationException {
    public Overlapping() {
      super("DELEGATION_OVERLAPPING",
          "The manager already has a delegation that overlaps the requested period.",
          HttpStatus.CONFLICT);
    }
  }

  public static class AlreadyFinished extends ManagerDelegationException {
    public AlreadyFinished() {
      super("DELEGATION_ALREADY_FINISHED",
          "The delegation has already been revoked or has expired.", HttpStatus.CONFLICT);
    }
  }
}
