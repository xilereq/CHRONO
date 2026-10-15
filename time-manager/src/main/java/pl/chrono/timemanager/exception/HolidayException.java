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

  public static class InvalidYear extends HolidayException {
    public InvalidYear(int year) {
      super(
          ErrorCodes.HOLIDAY_INVALID_YEAR,
          "Year " + year + " is out of supported range (1976-2076).",
          HttpStatus.BAD_REQUEST
      );
    }
  }

  public static class InvalidCountryCode extends HolidayException {
    public InvalidCountryCode(String countryCode) {
      super(
          ErrorCodes.HOLIDAY_INVALID_COUNTRY_CODE,
          "Country code '" + countryCode + "' is invalid or not supported by external provider.",
          HttpStatus.BAD_REQUEST
      );
    }
  }

  public static class ExternalApiError extends HolidayException {
    public ExternalApiError(String details) {
      super(
          ErrorCodes.HOLIDAY_EXTERNAL_API_ERROR,
          "Failed to fetch holidays from external provider: " + details,
          HttpStatus.BAD_GATEWAY
      );
    }
  }

  public static class AlreadyExists extends HolidayException {
    public AlreadyExists(String name, java.time.LocalDate date) {
      super(
          ErrorCodes.HOLIDAY_ALREADY_EXISTS,
          "Holiday '" + name + "' on date " + date + " already exists.",
          HttpStatus.CONFLICT
      );
    }
  }
}
