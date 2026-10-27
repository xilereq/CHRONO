package pl.chrono.reportmanager.exception;

import org.springframework.http.HttpStatus;

public class ReportScheduleSecurityException extends ReportException {

  protected ReportScheduleSecurityException(String errorCode, String message, HttpStatus status) {
    super(errorCode, message, status);
  }

  public static class UnauthorizedTeamReport extends ReportScheduleSecurityException {
    public UnauthorizedTeamReport() {
      super(
          ErrorCodes.UNAUTHORIZED_TEAM_REPORT,
          "Access denied. Employees cannot schedule team reports.",
          HttpStatus.FORBIDDEN
      );
    }
  }

  public static class UnauthorizedTargetUser extends ReportScheduleSecurityException {
    public UnauthorizedTargetUser() {
      super(
          ErrorCodes.UNAUTHORIZED_TARGET_USER,
          "Access denied. Employees can only schedule reports for themselves.",
          HttpStatus.FORBIDDEN
      );
    }
  }

  public static class UnauthorizedRecipientEmail extends ReportScheduleSecurityException {
    public UnauthorizedRecipientEmail() {
      super(
          ErrorCodes.UNAUTHORIZED_RECIPIENT_EMAIL,
          "Access denied. Employees can only send scheduled reports to their own email address.",
          HttpStatus.FORBIDDEN
      );
    }
  }

  public static class UnauthorizedManagerTargetUsers extends ReportScheduleSecurityException {
    public UnauthorizedManagerTargetUsers() {
      super(
          ErrorCodes.UNAUTHORIZED_MANAGER_TARGET_USERS,
          "Access denied. Managers can only schedule reports for themselves and their subordinates.",
          HttpStatus.FORBIDDEN
      );
    }
  }

  public static class UnauthorizedScheduleAccess extends ReportScheduleSecurityException {
    public UnauthorizedScheduleAccess() {
      super(
          ErrorCodes.UNAUTHORIZED_SCHEDULE_ACCESS,
          "Access denied. You can only manage your own scheduled reports.",
          HttpStatus.FORBIDDEN
      );
    }
  }
}
