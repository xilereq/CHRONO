package pl.chrono.timemanager.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class OvertimeException extends TimeManagerException {

  public OvertimeException(String errorCode, String message, HttpStatus status) {
    super(errorCode, message, status);
  }

  public static class InsufficientBalance extends OvertimeException {
    public InsufficientBalance(int currentBalance) {
      super(
          ErrorCodes.INSUFFICIENT_BALANCE,
          buildMessage(currentBalance),
          HttpStatus.BAD_REQUEST
      );
    }
  }

  private static String buildMessage(int currentBalance) {
    int currentBalanceHours = currentBalance / 3600;
    int currentBalanceMins = currentBalance % 3600 / 60;
    String formattedTime = String.format("%02dh:%02dm", currentBalanceHours, currentBalanceMins);
    return "Not enough overtime balance to reserve (you have currently have "
        + formattedTime + " to use.";
  }
}
