package pl.chrono.timemanager.exception;

import java.time.LocalDate;
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

  public static class InvalidManagerId extends TeamDashboardException {
    public InvalidManagerId(Integer managerId) {
      super(
          ErrorCodes.INVALID_MANAGER_ID,
          "Manager ID must be a positive number, got: " + managerId + ".",
          HttpStatus.BAD_REQUEST
      );
    }
  }

  public static class DateOutOfRange extends TeamDashboardException {
    public DateOutOfRange(LocalDate date, LocalDate min, LocalDate max) {
      super(
          ErrorCodes.DASHBOARD_DATE_OUT_OF_RANGE,
          "Dashboard date " + date + " must be between " + min + " and " + max + ".",
          HttpStatus.BAD_REQUEST
      );
    }
  }
}
