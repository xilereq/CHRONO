package pl.chrono.applicationmanager.exception;

import org.springframework.http.HttpStatus;

public class ApplicationException extends ApplicationManagerException {

  public ApplicationException(String errorCode, String message, HttpStatus status) {
    super(errorCode, message, status);
  }

  public static class NotFound extends ApplicationException {
    public NotFound(Long applicationId) {
      super(
          ErrorCodes.APPLICATION_NOT_FOUND,
          "Application with ID " + applicationId + " does not exist.",
          HttpStatus.NOT_FOUND
      );
    }
  }

  public static class AlreadyProcessed extends ApplicationException {
    public AlreadyProcessed(String status) {
      super(
          ErrorCodes.APPLICATION_ALREADY_PROCESSED,
          "Application has already been processed (status: " + status + ") and its status cannot be changed.",
          HttpStatus.CONFLICT
      );
    }
  }

  public static class NotPending extends ApplicationException {
    public NotPending() {
      super(
          ErrorCodes.APPLICATION_NOT_PENDING,
          "Only applications with PENDING status can be canceled.",
          HttpStatus.CONFLICT
      );
    }
  }
}
