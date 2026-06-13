package pl.chrono.timemanager.exception;

public final class ErrorCodes {
  public static final String POLICY_NOT_FOUND = "POLICY_NOT_FOUND";
  public static final String PAST_START_DATE = "PAST_START_DATE";
  public static final String START_DATE_CONFLICT = "START_DATE_CONFLICT";
  public static final String POLICY_ALREADY_ASSIGNED = "POLICY_ALREADY_ASSIGNED";

  public static final String LEAVE_ALREADY_STARTED = "LEAVE_ALREADY_STARTED";
  public static final String NO_ACTIVE_LEAVE = "NO_ACTIVE_LEAVE";
  public static final String NO_ACTIVE_WORK = "NO_ACTIVE_WORK";
  public static final String WORK_ALREADY_STARTED = "WORK_ALREADY_STARTED";
  public static final String INSUFFICIENT_WORK_TIME = "INSUFFICIENT_WORK_TIME";
  public static final String INSUFFICIENT_BALANCE = "INSUFFICIENT_BALANCE";

  public static final String HOLIDAY_NOT_FOUND = "HOLIDAY_NOT_FOUND";
  public static final String HOLIDAY_ALREADY_ASSIGNED = "HOLIDAY_ALREADY_ASSIGNED";
  public static final String HOLIDAY_NOT_ASSIGNED = "HOLIDAY_NOT_ASSIGNED";

  public static final String REQUEST_NOT_FOUND = "REQUEST_NOT_FOUND";
  public static final String INVALID_TIME_RANGE = "INVALID_TIME_RANGE";
  public static final String DAILY_LIMIT_EXCEEDED = "DAILY_LIMIT_EXCEEDED";
  public static final String MISSING_TARGET_WORKLOG = "MISSING_TARGET_WORKLOG";
  public static final String TARGET_WORKLOG_NOT_FOUND = "TARGET_WORKLOG_NOT_FOUND";
  public static final String UNAUTHORIZED_WORKLOG_ACCESS = "UNAUTHORIZED_WORKLOG_ACCESS";
  public static final String INVALID_REQUEST_STATE = "INVALID_REQUEST_STATE";
  public static final String UNAUTHORIZED_MANAGER = "UNAUTHORIZED_MANAGER";
  public static final String OVERLAPPING_WORKLOG = "OVERLAPPING_WORKLOG";

  public static final String USER_NOT_FOUND = "USER_NOT_FOUND";

  public static final String APPLICATION_DAILY_LIMIT_EXCEEDED = "APPLICATION_DAILY_LIMIT_EXCEEDED";
  public static final String REQUESTED_TIME_EXCEEDS_SCHEDULED = "REQUESTED_TIME_EXCEEDS_SCHEDULED";
}
