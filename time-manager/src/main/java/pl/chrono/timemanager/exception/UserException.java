package pl.chrono.timemanager.exception;

import org.springframework.http.HttpStatus;

public class UserException extends TimeManagerException {
  public UserException(String errorCode, String message, HttpStatus status) {
    super(errorCode, message, status);
  }

  public static class NotFound extends UserException {
    public NotFound(int requestId) {
      super(
          ErrorCodes.USER_NOT_FOUND,
          "User with ID " + requestId + " was not found.",
          HttpStatus.NOT_FOUND
      );
    }
  }
}
