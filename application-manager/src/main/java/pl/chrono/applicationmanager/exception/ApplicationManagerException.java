package pl.chrono.applicationmanager.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class ApplicationManagerException extends RuntimeException {
  private final String errorCode;
  private final HttpStatus status;

  public ApplicationManagerException(String errorCode, String message, HttpStatus status) {
    super(message);
    this.errorCode = errorCode;
    this.status = status;
  }
}
