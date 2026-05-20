package pl.chrono.timemanager.exception;

import lombok.Getter;

@Getter
public class PolicyException extends RuntimeException {
  private final String errorCode;

  public PolicyException(String errorCode, String message) {
    super(message);
    this.errorCode = errorCode;
  }
  public static class NotFound extends PolicyException {
    public NotFound(Integer policyId) {
      super("POLICY_NOT_FOUND", "Policy with ID " + policyId + " does not exist!");
    }
  }
}
