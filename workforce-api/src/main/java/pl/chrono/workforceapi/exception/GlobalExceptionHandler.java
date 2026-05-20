package pl.chrono.workforceapi.exception;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import pl.chrono.timemanager.exception.TimeManagerException;

@RestControllerAdvice
@SuppressWarnings("unused")
public class GlobalExceptionHandler {

  @ExceptionHandler(TimeManagerException.class)
  public ResponseEntity<Object> handleTimeManagerException(TimeManagerException ex) {
    Map<String, Object> body = new HashMap<>();
    body.put("code", ex.getErrorCode());
    body.put("message", ex.getMessage());
    body.put("timestamp", LocalDateTime.now());
    return new ResponseEntity<>(body, ex.getStatus());
  }

  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<Object> handleIllegalArgumentException(IllegalArgumentException ex) {
    Map<String, Object> body = new HashMap<>();
    body.put("message", ex.getMessage());
    body.put("timestamp", LocalDateTime.now());
    return new ResponseEntity<>(body, HttpStatus.BAD_REQUEST);
  }
}
