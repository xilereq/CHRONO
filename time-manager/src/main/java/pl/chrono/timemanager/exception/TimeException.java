package pl.chrono.timemanager.exception;

import lombok.Getter;

@Getter
public class TimeException extends RuntimeException {
  private final String errorCode;

  protected TimeException(String errorCode, String message) {
    super(message);
    this.errorCode = errorCode;
  }
}
