package pl.chrono.reportmanager.exception;

import org.springframework.http.HttpStatus;

public abstract class ReportException extends RuntimeException {
  private final String errorCode;
  private final HttpStatus status;

  protected ReportException(String errorCode, String message, HttpStatus status) {
    super(message);
    this.errorCode = errorCode;
    this.status = status;
  }

  protected ReportException(String errorCode, String message, HttpStatus status, Throwable cause) {
    super(message, cause);
    this.errorCode = errorCode;
    this.status = status;
  }

  public String getErrorCode() { return errorCode; }
  public HttpStatus getStatus() { return status; }
}
