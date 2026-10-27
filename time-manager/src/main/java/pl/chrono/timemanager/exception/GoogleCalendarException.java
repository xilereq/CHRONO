package pl.chrono.timemanager.exception;

import org.springframework.http.HttpStatus;

public class GoogleCalendarException extends TimeManagerException {

  public GoogleCalendarException(String errorCode, String message, HttpStatus status) {
    super(errorCode, message, status);
  }

  public static class InvalidState extends GoogleCalendarException {
    public InvalidState() {
      super(
          ErrorCodes.GOOGLE_INVALID_STATE,
          "Invalid or expired Google OAuth state parameter.",
          HttpStatus.BAD_REQUEST
      );
    }
  }

  public static class MissingRefreshToken extends GoogleCalendarException {
    public MissingRefreshToken() {
      super(
          ErrorCodes.GOOGLE_MISSING_REFRESH_TOKEN,
          "Google OAuth response did not contain a refresh token.",
          HttpStatus.BAD_REQUEST
      );
    }
  }

  public static class NotConfigured extends GoogleCalendarException {
    public NotConfigured() {
      super(
          ErrorCodes.GOOGLE_NOT_CONFIGURED,
          "Google Calendar integration is not properly configured.",
          HttpStatus.INTERNAL_SERVER_ERROR
      );
    }
  }

  public static class ExternalApiError extends GoogleCalendarException {
    public ExternalApiError(String details) {
      super(
          ErrorCodes.GOOGLE_EXTERNAL_API_ERROR,
          "Failed to communicate with Google Calendar API: " + details,
          HttpStatus.BAD_GATEWAY
      );
    }
  }
}
