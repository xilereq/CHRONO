package pl.chrono.workforceapi.exception;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import lombok.NonNull;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import pl.chrono.applicationmanager.exception.ApplicationManagerException;
import pl.chrono.timemanager.exception.TimeManagerException;

@RestControllerAdvice
@SuppressWarnings("unused")
public class GlobalExceptionHandler {

  @ExceptionHandler(TimeManagerException.class)
  public ResponseEntity<Object> handleTimeManagerException(TimeManagerException ex) {
    return buildErrorResponse(ex.getErrorCode(), ex.getMessage(), ex.getStatus());
  }

  @ExceptionHandler(ApplicationManagerException.class)
  public ResponseEntity<Object> handleApplicationManagerException(ApplicationManagerException ex) {
    return buildErrorResponse(ex.getErrorCode(), ex.getMessage(), ex.getStatus());
  }

  @NonNull
  private ResponseEntity<Object> buildErrorResponse(String errorCode, String message, HttpStatus status) {
    Map<String, Object> body = new HashMap<>();
    body.put("code", errorCode);
    body.put("message", message);
    body.put("timestamp", LocalDateTime.now());
    return new ResponseEntity<>(body, status);
  }

  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<Object> handleIllegalArgumentException(IllegalArgumentException ex) {
    Map<String, Object> body = new HashMap<>();
    body.put("message", ex.getMessage());
    body.put("timestamp", LocalDateTime.now());
    return new ResponseEntity<>(body, HttpStatus.BAD_REQUEST);
  }
}
