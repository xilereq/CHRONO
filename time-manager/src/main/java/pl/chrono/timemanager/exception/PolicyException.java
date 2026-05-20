package pl.chrono.timemanager.exception;

import org.springframework.http.HttpStatus;

public class PolicyException extends TimeManagerException {

  public PolicyException(String errorCode, String message, HttpStatus status) {
    super(errorCode, message, status);
  }
  public static class NotFound extends PolicyException {
    public NotFound(Integer policyId) {
      super("POLICY_NOT_FOUND",
          "Policy with ID " + policyId + " does not exist!", HttpStatus.NOT_FOUND);
    }
  }
}
