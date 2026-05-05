package pl.chrono.timemanager.exception;

public class NoActiveLeaveException extends TimeException {
  public NoActiveLeaveException(Integer userId) {
    super("NO_ACTIVE_LEAVE", "User with ID " + userId + " is not on leave.");
  }
}
