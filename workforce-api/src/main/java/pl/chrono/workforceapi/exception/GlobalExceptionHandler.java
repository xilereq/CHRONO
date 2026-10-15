package pl.chrono.workforceapi.exception;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.NonNull;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import pl.chrono.applicationmanager.exception.ApplicationManagerException;
import pl.chrono.timemanager.exception.TimeManagerException;
import pl.chrono.reportmanager.exception.ReportException;

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

  @ExceptionHandler(ReportException.class)
  public ResponseEntity<Object> handleReportException(ReportException ex) {
    return buildErrorResponse(ex.getErrorCode(), ex.getMessage(), ex.getStatus());
  }

  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<Object> handleIllegalArgumentException(IllegalArgumentException ex) {
    return buildErrorResponse("ILLEGAL_ARGUMENT", ex.getMessage(), HttpStatus.BAD_REQUEST);
  }

  @ExceptionHandler(AuthorizationDeniedException.class)
  public ResponseEntity<Object> handleAuthorizationDeniedException(AuthorizationDeniedException ex) {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth == null || auth instanceof AnonymousAuthenticationToken) {
      return buildErrorResponse(
          "UNAUTHORIZED",
          "Authentication is required to access this resource",
          HttpStatus.UNAUTHORIZED
      );
    }
    return buildErrorResponse("ACCESS_DENIED", "Access Denied", HttpStatus.FORBIDDEN);
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<Object> handleValidationException(MethodArgumentNotValidException ex) {
    String message = ex.getBindingResult().getFieldErrors().stream()
        .map(err -> err.getField() + ": " + err.getDefaultMessage())
        .collect(Collectors.joining(", "));
    return buildErrorResponse("VALIDATION_ERROR", message, HttpStatus.BAD_REQUEST);
  }

  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ResponseEntity<Object> handleHttpMessageNotReadableException(
      HttpMessageNotReadableException ex) {
    return buildErrorResponse(
        "MALFORMED_JSON_REQUEST",
        "Invalid request body format or data types",
        HttpStatus.BAD_REQUEST
    );
  }

  @NonNull
  private ResponseEntity<Object> buildErrorResponse(String errorCode, String message, HttpStatus status) {
    Map<String, Object> body = new HashMap<>();
    body.put("code", errorCode);
    body.put("message", message);
    body.put("timestamp", LocalDateTime.now());
    return new ResponseEntity<>(body, status);
  }
}
