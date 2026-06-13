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

  public static class MissingTargetWorkLog extends WorkLogRequestException {
    public MissingTargetWorkLog() {
      super(
          ErrorCodes.MISSING_TARGET_WORKLOG,
          "Target WorkLog ID is required for UPDATE requests.",
          HttpStatus.BAD_REQUEST
      );
    }
  }

  public static class TargetWorkLogNotFound extends WorkLogRequestException {
    public TargetWorkLogNotFound() {
      super(
          ErrorCodes.TARGET_WORKLOG_NOT_FOUND,
          "Target WorkLog for correction was not found.",
          HttpStatus.NOT_FOUND
      );
    }
  }

  public static class UnauthorizedWorkLogAccess extends WorkLogRequestException {
    public UnauthorizedWorkLogAccess() {
      super(
          ErrorCodes.UNAUTHORIZED_WORKLOG_ACCESS,
          "You cannot request changes to another user's WorkLog.",
          HttpStatus.FORBIDDEN
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

  public static class UnauthorizedManager extends WorkLogRequestException {
    public UnauthorizedManager() {
      super(
          ErrorCodes.UNAUTHORIZED_MANAGER,
          "Access Denied. Only the direct manager can process this request.",
          HttpStatus.FORBIDDEN
      );
    }
  }

  public static class OverlappingWorkLog extends WorkLogRequestException {
    public OverlappingWorkLog() {
      super(
          ErrorCodes.OVERLAPPING_WORKLOG,
          "Proposed time range overlaps with an existing work log.",
          HttpStatus.CONFLICT
      );
    }
  }
}
