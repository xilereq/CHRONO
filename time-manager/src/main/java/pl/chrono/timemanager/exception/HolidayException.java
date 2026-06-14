package pl.chrono.timemanager.exception;

import org.springframework.http.HttpStatus;

public class HolidayException extends TimeManagerException {

  public HolidayException(String errorCode, String message, HttpStatus status) {
    super(errorCode, message, status);
  }

  public static class NotFound extends HolidayException {
    public NotFound(Integer holidayId) {
      super(
          ErrorCodes.HOLIDAY_NOT_FOUND, "Holiday with ID " + holidayId + " does not exist!",
          HttpStatus.NOT_FOUND
      );
    }
  }

  public static class AlreadyAssigned extends HolidayException {
    public AlreadyAssigned() {
      super(ErrorCodes.HOLIDAY_ALREADY_ASSIGNED,
          "This holiday has been already assigned to this policy.", HttpStatus.CONFLICT);
    }
  }

  public static class NotAssigned extends HolidayException {
    public NotAssigned() {
      super(ErrorCodes.HOLIDAY_NOT_ASSIGNED,
          "This holiday is not assigned to the specified policy.", HttpStatus.NOT_FOUND);
    }
  }

  public static class NoneFound extends HolidayException {
    public NoneFound() {
      super(ErrorCodes.HOLIDAY_NONE_FOUND,
          "No valid holidays found for the provided IDs.", HttpStatus.NOT_FOUND);
    }
  }
}

