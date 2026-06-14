package pl.chrono.reportmanager.exception;

import org.springframework.http.HttpStatus;

public class ReportGenerationException extends ReportException {

  protected ReportGenerationException(String errorCode, String message, HttpStatus status) {
    super(errorCode, message, status);
  }

  protected ReportGenerationException(String errorCode, String message, HttpStatus status, Throwable cause) {
    super(errorCode, message, status, cause);
  }

  public static class UnsupportedFormat extends ReportGenerationException {
    public UnsupportedFormat(String format) {
      super(
          "UNSUPPORTED_REPORT_FORMAT",
          "Export to " + format + " is currently not supported. Missing required library.",
          HttpStatus.BAD_REQUEST
      );
    }
  }

  public static class Failed extends ReportGenerationException {
    public Failed(String format, Throwable cause) {
      super(
          "REPORT_GENERATION_FAILED",
          "An error occurred while generating the report in format: " + format,
          HttpStatus.INTERNAL_SERVER_ERROR,
          cause
      );
    }
  }
}
