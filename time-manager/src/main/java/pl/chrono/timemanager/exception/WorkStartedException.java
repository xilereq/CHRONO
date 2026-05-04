package pl.chrono.timemanager.exception;

public class WorkStartedException extends TimeException {
  public WorkStartedException(Integer userId) {
    super("WORK_ALREADY_STARTED", "User with ID " + userId + " is already working.");
  }
}
