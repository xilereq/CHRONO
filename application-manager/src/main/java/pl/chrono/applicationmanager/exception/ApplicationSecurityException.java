package pl.chrono.applicationmanager.exception;

import org.springframework.http.HttpStatus;

public class ApplicationSecurityException extends ApplicationManagerException {

  public ApplicationSecurityException(String errorCode, String message, HttpStatus status) {
    super(errorCode, message, status);
  }

  public static class UnauthorizedManager extends ApplicationSecurityException {
    public UnauthorizedManager() {
      super(
          ErrorCodes.UNAUTHORIZED_MANAGER,
          "Access denied. Only the direct manager can process this employee's applications.",
          HttpStatus.FORBIDDEN
      );
    }
  }

  public static class UnauthorizedAccess extends ApplicationSecurityException {
    public UnauthorizedAccess() {
      super(
          ErrorCodes.UNAUTHORIZED_ACCESS,
          "Access denied. You can only view your own applications or those of your subordinates.",
          HttpStatus.FORBIDDEN
      );
    }
  }

  public static class UnauthorizedCancellation extends ApplicationSecurityException {
    public UnauthorizedCancellation() {
      super(
          ErrorCodes.UNAUTHORIZED_CANCELLATION,
          "Access denied. You can only cancel your own applications.",
          HttpStatus.FORBIDDEN
      );
    }
  }
}
