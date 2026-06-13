package pl.chrono.applicationmanager.exception;

import org.springframework.http.HttpStatus;

public class ApplicationUserException extends ApplicationManagerException {

  public ApplicationUserException(String errorCode, String message, HttpStatus status) {
    super(errorCode, message, status);
  }

  public static class NotFound extends ApplicationUserException {
    public NotFound(Integer userId) {
      super(
          ErrorCodes.USER_NOT_FOUND,
          "User with ID " + userId + " was not found.",
          HttpStatus.NOT_FOUND
      );
    }
  }
}
