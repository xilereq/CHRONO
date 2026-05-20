package pl.chrono.timemanager.exception;

import org.springframework.http.HttpStatus;

public class PolicyException extends TimeManagerException {

  public PolicyException(String errorCode, String message, HttpStatus status) {
    super(errorCode, message, status);
  }

  public static class NotFound extends PolicyException {
    public NotFound(Integer policyId) {
      super(
          ErrorCodes.POLICY_NOT_FOUND, "Policy with ID " + policyId + " does not exist!",
          HttpStatus.NOT_FOUND
      );
    }
  }

  public static class InvalidDate extends PolicyException {
    public InvalidDate(String code, String message) {
      super(code, message, HttpStatus.BAD_REQUEST);
    }
  }

  public static class AlreadyAssigned extends PolicyException {
    public AlreadyAssigned(String code, String message) {
      super(code, message, HttpStatus.CONFLICT);
    }
  }
}
