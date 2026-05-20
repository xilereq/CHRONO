package pl.chrono.timemanager.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class WorkException extends TimeManagerException {

  public WorkException(String errorCode, String message, HttpStatus status) {
    super(errorCode, message, status);
  }

  public static class LeaveStarted extends WorkException {
    public LeaveStarted(Integer userId) {
      super(
          ErrorCodes.LEAVE_ALREADY_STARTED, "User with ID " + userId + " is already on a leave.",
          HttpStatus.CONFLICT
      );
    }
  }

  public static class NoActiveLeave extends WorkException {
    public NoActiveLeave(Integer userId) {
      super(
          ErrorCodes.NO_ACTIVE_LEAVE, "User with ID " + userId + " is not on leave.",
          HttpStatus.CONFLICT
      );
    }
  }

  public static class NoActiveWork extends WorkException {
    public NoActiveWork(Integer userId) {
      super(
          ErrorCodes.NO_ACTIVE_WORK, "User with ID " + userId + " is not working.",
          HttpStatus.CONFLICT
      );
    }
  }

  public static class WorkStarted extends WorkException {
    public WorkStarted(Integer userId) {
      super(
          ErrorCodes.WORK_ALREADY_STARTED, "User with ID " + userId + " is already working.",
          HttpStatus.CONFLICT
      );
    }
  }
}
