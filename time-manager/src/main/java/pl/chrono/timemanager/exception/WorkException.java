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

  public static class InsufficientWorkTime extends WorkException {
    public InsufficientWorkTime(String formattedTime) {
      super(
          ErrorCodes.INSUFFICIENT_WORK_TIME,
          "You still need to work for: " + formattedTime + " today.",
          HttpStatus.BAD_REQUEST
      );
    }
  }

  public static class ExcessiveWorkTime extends WorkException {
    public ExcessiveWorkTime(int requiredSeconds, int maxAllowedSeconds) {
      super(
          ErrorCodes.EXCESSIVE_WORK_TIME, buildMessage(requiredSeconds, maxAllowedSeconds),
          HttpStatus.CONFLICT
      );
    }

    private static String buildMessage(int requiredSeconds, int maxAllowedSeconds) {
      int requiredHours = requiredSeconds / 3600;
      int requiredMinutes = requiredSeconds % 3600 / 60;
      String formattedTime = String.format("%02dh:%02dm", requiredHours, requiredMinutes);
      int maxHours = maxAllowedSeconds / 3600;
      String maxFormattedTime = String.format("%02dh", maxHours);
      return "You have exceeded maximum work time for your policy: " + maxFormattedTime +
          ". Your work time has automatically been set to " + formattedTime + ".";
    }
  }
}
