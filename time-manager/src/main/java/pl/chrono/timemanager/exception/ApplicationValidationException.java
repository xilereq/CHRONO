package pl.chrono.timemanager.exception;

import org.springframework.http.HttpStatus;

public class ApplicationValidationException extends TimeManagerException {

  public ApplicationValidationException(String errorCode, String message, HttpStatus status) {
    super(errorCode, message, status);
  }

  public static class DailyLimitExceeded extends ApplicationValidationException {
    public DailyLimitExceeded(int maxAllowedHours) {
      super(
          ErrorCodes.APPLICATION_DAILY_LIMIT_EXCEEDED,
          "Application rejected. Standard working hours plus requested overtime exceed the maximum daily limit of " + maxAllowedHours + "h.",
          HttpStatus.BAD_REQUEST
      );
    }
  }

  public static class RequestedTimeExceedsScheduled extends ApplicationValidationException {
    public RequestedTimeExceedsScheduled(int requestedHours, int scheduledHours) {
      super(
          ErrorCodes.REQUESTED_TIME_EXCEEDS_SCHEDULED,
          "You cannot request more hours off (" + requestedHours + "h) than your scheduled working hours for this day (" + scheduledHours + "h).",
          HttpStatus.BAD_REQUEST
      );
    }
  }
}
