package pl.chrono.reportmanager.exception;

public record ErrorCodes() {
  public static final String USER_NOT_FOUND = "USER_NOT_FOUND";
  public static final String UNSUPPORTED_REPORT_FORMAT = "UNSUPPORTED_REPORT_FORMAT";
  public static final String REPORT_GENERATION_FAILED = "REPORT_GENERATION_FAILED";

  public static final String SCHEDULE_NOT_FOUND = "SCHEDULE_NOT_FOUND";
  public static final String TARGET_USERS_NOT_FOUND = "TARGET_USERS_NOT_FOUND";
  public static final String MISSING_DAY_OF_WEEK = "MISSING_DAY_OF_WEEK";
  public static final String MISSING_DAY_OF_MONTH = "MISSING_DAY_OF_MONTH";
  public static final String INVALID_CUSTOM_DATE_RANGE = "INVALID_CUSTOM_DATE_RANGE";

  public static final String UNAUTHORIZED_TEAM_REPORT = "UNAUTHORIZED_TEAM_REPORT";
  public static final String UNAUTHORIZED_TARGET_USER = "UNAUTHORIZED_TARGET_USER";
  public static final String UNAUTHORIZED_RECIPIENT_EMAIL = "UNAUTHORIZED_RECIPIENT_EMAIL";
  public static final String UNAUTHORIZED_MANAGER_TARGET_USERS =
      "UNAUTHORIZED_MANAGER_TARGET_USERS";
  public static final String UNAUTHORIZED_SCHEDULE_ACCESS = "UNAUTHORIZED_SCHEDULE_ACCESS";
}
