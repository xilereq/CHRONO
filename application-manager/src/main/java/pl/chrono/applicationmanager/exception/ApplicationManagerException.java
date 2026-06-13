package pl.chrono.applicationmanager.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public abstract class ApplicationManagerException extends RuntimeException {
  private final String errorCode;
  private final HttpStatus status;

  protected ApplicationManagerException(String errorCode, String message, HttpStatus status) {
    super(message);
    this.errorCode = errorCode;
    this.status = status;
  }
}
