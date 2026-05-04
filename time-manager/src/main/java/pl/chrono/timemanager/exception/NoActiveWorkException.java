package pl.chrono.timemanager.exception;

public class NoActiveWorkException extends TimeException {
  public NoActiveWorkException(Integer userId) {
    super("NO_ACTIVE_WORK", "User with ID " + userId + " is not working.");
  }
}
