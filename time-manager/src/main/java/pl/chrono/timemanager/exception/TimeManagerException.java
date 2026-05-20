package pl.chrono.timemanager.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public abstract class TimeManagerException extends RuntimeException {
  private final String errorCode;
  private final HttpStatus status;

  protected TimeManagerException(String errorCode, String message, HttpStatus status) {
    super(message);
    this.errorCode = errorCode;
    this.status = status;
  }
}
