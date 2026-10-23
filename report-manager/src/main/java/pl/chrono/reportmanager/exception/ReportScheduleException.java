package pl.chrono.reportmanager.exception;

import java.util.Collection;
import org.springframework.http.HttpStatus;

public class ReportScheduleException extends ReportException {

  protected ReportScheduleException(String errorCode, String message, HttpStatus status) {
    super(errorCode, message, status);
  }

  public static class NotFound extends ReportScheduleException {
    public NotFound(Long scheduleId) {
      super(
          ErrorCodes.SCHEDULE_NOT_FOUND,
          "Scheduled report with ID " + scheduleId + " does not exist.",
          HttpStatus.NOT_FOUND
      );
    }
  }

  public static class UsersNotFound extends ReportScheduleException {
    public UsersNotFound(Collection<Integer> missingIds) {
      super(
          ErrorCodes.TARGET_USERS_NOT_FOUND,
          "Users with IDs " + missingIds + " not found.",
          HttpStatus.NOT_FOUND
      );
    }
  }

  public static class MissingDayOfWeek extends ReportScheduleException {
    public MissingDayOfWeek() {
      super(
          ErrorCodes.MISSING_DAY_OF_WEEK,
          "Day of week is required for WEEKLY frequency.",
          HttpStatus.BAD_REQUEST
      );
    }
  }

  public static class MissingDayOfMonth extends ReportScheduleException {
    public MissingDayOfMonth() {
      super(
          ErrorCodes.MISSING_DAY_OF_MONTH,
          "Day of month is required for MONTHLY frequency.",
          HttpStatus.BAD_REQUEST
      );
    }
  }

  public static class InvalidCustomDateRange extends ReportScheduleException {
    public InvalidCustomDateRange() {
      super(
          ErrorCodes.INVALID_CUSTOM_DATE_RANGE,
          "Valid dateFrom and dateTo (dateFrom <= dateTo) are required for CUSTOM frequency.",
          HttpStatus.BAD_REQUEST
      );
    }
  }
}
