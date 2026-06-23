package pl.chrono.timemanager.exception;

import org.springframework.http.HttpStatus;

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

  public static class CancellationExpired extends WorkException {
    public CancellationExpired() {
      super(
          ErrorCodes.CANCELLATION_EXPIRED,
          "You can cancel work only withing starting 15 minutes.",
          HttpStatus.BAD_REQUEST
      );
    }
  }

  public static class NoHomeOfficeApplication extends WorkException {
    public NoHomeOfficeApplication() {
      super(
          ErrorCodes.NO_HO_APPLICATION,
          "You do not have approved application for home office for today.",
          HttpStatus.FORBIDDEN
      );
    }
  }

  public static class AlreadyWorkedToday extends WorkException {
    public AlreadyWorkedToday() {
      super(
          ErrorCodes.ALREADY_WORKED_TODAY,
          "You have already worked today.",
          HttpStatus.FORBIDDEN
      );
    }
  }

  public static class OutsideEmploymentPeriod extends WorkException {
    public OutsideEmploymentPeriod() {
      super(
          ErrorCodes.OUTSIDE_EMPLOYMENT_PERIOD,
          "The requested date is outside of your employment period.",
          HttpStatus.FORBIDDEN
      );
    }
  }
}
