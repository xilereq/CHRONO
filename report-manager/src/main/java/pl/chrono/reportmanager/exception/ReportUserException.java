package pl.chrono.reportmanager.exception;

import org.springframework.http.HttpStatus;

public class ReportUserException extends ReportException {

  protected ReportUserException(String errorCode, String message, HttpStatus status) {
    super(errorCode, message, status);
  }

  public static class NotFound extends ReportUserException {
    public NotFound(Integer userId) {
      super(ErrorCodes.USER_NOT_FOUND, "User with ID " + userId + " not found.", HttpStatus.NOT_FOUND);
    }
  }

  public static class AccessDenied extends ReportUserException {
    public AccessDenied(Integer employeeId) {
      super(ErrorCodes.REPORT_ACCESS_DENIED,
          "You are not allowed to view reports of user with ID " + employeeId + ".",
          HttpStatus.FORBIDDEN);
    }
  }
}
