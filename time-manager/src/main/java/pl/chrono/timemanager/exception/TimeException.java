package pl.chrono.timemanager.exception;

import lombok.Getter;

@Getter
public class TimeException extends RuntimeException {
  private final String errorCode;

  public TimeException(String errorCode, String message) {
    super(message);
    this.errorCode = errorCode;
  }

  public static class LeaveStarted extends TimeException {
    public LeaveStarted(Integer userId) {
      super("LEAVE_ALREADY_STARTED", "User with ID " + userId + " is already on a leave.");
    }
  }

  public static class NoActiveLeave extends TimeException {
    public NoActiveLeave(Integer userId) {
      super("NO_ACTIVE_LEAVE", "User with ID " + userId + " is not on leave.");
    }
  }

  public static class NoActiveWork extends TimeException {
    public NoActiveWork(Integer userId) {
      super("NO_ACTIVE_WORK", "User with ID " + userId + " is not working.");
    }
  }

  public static class WorkStarted extends TimeException {
    public WorkStarted(Integer userId) {
      super("WORK_ALREADY_STARTED", "User with ID " + userId + " is already working.");
    }
  }

}
