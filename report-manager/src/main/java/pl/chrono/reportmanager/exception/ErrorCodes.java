package pl.chrono.reportmanager.exception;

public record ErrorCodes() {
  public static final String USER_NOT_FOUND = "USER_NOT_FOUND";
  public static final String UNSUPPORTED_REPORT_FORMAT = "UNSUPPORTED_REPORT_FORMAT";
  public static final String REPORT_GENERATION_FAILED = "REPORT_GENERATION_FAILED";
}
