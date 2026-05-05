package pl.chrono.timemanager.exception;

public class LeaveStartedException extends TimeException {
  public LeaveStartedException(Integer userId) {
    super("LEAVE_ALREADY_STARTED", "User with ID " + userId + " is already on a leave.");
  }
}
