package pl.chrono.timemanager.exception;

import org.springframework.http.HttpStatus;

public class TeamDashboardException extends TimeManagerException {

  public TeamDashboardException(String errorCode, String message, HttpStatus status) {
    super(errorCode, message, status);
  }

  public static class AccessDenied extends TeamDashboardException {
    public AccessDenied(Integer managerId) {
      super(
          ErrorCodes.UNAUTHORIZED_MANAGER,
          "Access Denied. You cannot view the team dashboard of manager " + managerId + ".",
          HttpStatus.FORBIDDEN
      );
    }
  }
}
