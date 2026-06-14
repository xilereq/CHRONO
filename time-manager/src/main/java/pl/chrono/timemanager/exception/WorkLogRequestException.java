package pl.chrono.timemanager.exception;

import org.springframework.http.HttpStatus;

public class WorkLogRequestException extends TimeManagerException {

  public WorkLogRequestException(String errorCode, String message, HttpStatus status) {
    super(errorCode, message, status);
  }

  public static class NotFound extends WorkLogRequestException {
    public NotFound(Long requestId) {
      super(
          ErrorCodes.REQUEST_NOT_FOUND,
          "WorkLogRequest with ID " + requestId + " does not exist.",
          HttpStatus.NOT_FOUND
      );
    }
  }

  public static class InvalidTimeRange extends WorkLogRequestException {
    public InvalidTimeRange() {
      super(
          ErrorCodes.INVALID_TIME_RANGE,
          "Proposed start time must be before end time.",
          HttpStatus.BAD_REQUEST
      );
    }
  }

  public static class DailyLimitExceeded extends WorkLogRequestException {
    public DailyLimitExceeded(int maxDailyHours) {
      super(
          ErrorCodes.DAILY_LIMIT_EXCEEDED,
          "Requested work time exceeds maximum allowed daily hours (" + maxDailyHours + "h).",
          HttpStatus.BAD_REQUEST
      );
    }
  }

  public static class InvalidState extends WorkLogRequestException {
    public InvalidState() {
      super(
          ErrorCodes.INVALID_REQUEST_STATE,
          "Only PENDING requests can be processed.",
          HttpStatus.CONFLICT
      );
    }
  }

  public static class InsufficientTime extends WorkLogRequestException {
    public InsufficientTime(String requiredHours) {
      super(
          ErrorCodes.INSUFFICIENT_REQUEST_TIME,
          "Your work log needs to be at least " + requiredHours + " long.",
          HttpStatus.BAD_REQUEST
      );
    }
  }

  public static class UnauthorizedManager extends WorkLogRequestException {
    public UnauthorizedManager() {
      super(
          ErrorCodes.UNAUTHORIZED_MANAGER,
          "Access Denied. Only the direct manager can process this request.",
          HttpStatus.FORBIDDEN
      );
    }
  }
}
