package pl.chrono.timemanager.service;

import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.chrono.dbmanager.model.Policy;
import pl.chrono.dbmanager.model.User;
import pl.chrono.dbmanager.model.enums.ApplicationType;
import pl.chrono.dbmanager.ports.I_UserData;
import pl.chrono.timemanager.api.I_ApplicationValidationService;
import pl.chrono.timemanager.exception.ApplicationValidationException;
import pl.chrono.timemanager.exception.UserException;

@Service
@RequiredArgsConstructor
public class ApplicationValidationService implements I_ApplicationValidationService {

  private final I_UserData userData;
  private final LeaveBalanceService leaveBalanceService;
  private final WorkTimeCalculationService workingDaysCalculationService;

  @Override
  @Transactional(readOnly = true)
  public void validateApplicationLimits(Integer employeeId, LocalDate startDate, LocalDate endDate, ApplicationType type, Integer durationSeconds) {

    User user = userData.findById(employeeId)
        .orElseThrow(() -> new UserException.NotFound(employeeId));

    workingDaysCalculationService.validateEmploymentPeriod(user, startDate, endDate);

    Policy policy = user.getActivePolicy();

    int baseRequiredSeconds = policy != null ? policy.getRequiredSecondsForDate(startDate) : 8 * 3600;
    int maxAllowedSeconds = policy != null && policy.getMaxDailyHours() != null
        ? policy.getMaxDailyHours() * 3600 : 12 * 3600;

    switch (type) {
      case HOLIDAY_LEAVE:
        int requestedWorkingDays = workingDaysCalculationService.calculateWorkingDays(
            startDate, endDate, policy);
        if (requestedWorkingDays <= 0) return;

        int currentYear = startDate.getYear();

        int annualLimit = leaveBalanceService.calculateAnnualLeaveLimit(user);
        int rolloverDays = leaveBalanceService.getRolloverDaysFromPreviousYear(employeeId, currentYear);
        int usedDays = leaveBalanceService.getUsedLeaveDaysInYear(employeeId, currentYear);

        int totalAvailableDays = annualLimit + rolloverDays - usedDays;

        if (requestedWorkingDays > totalAvailableDays) {
          throw new ApplicationValidationException.InsufficientLeaveBalance(requestedWorkingDays, totalAvailableDays);
        }
        break;

      case OVERTIME_EARN:
        if (durationSeconds == null || durationSeconds <= 0) return;
        if (baseRequiredSeconds + durationSeconds > maxAllowedSeconds) {
          throw new ApplicationValidationException.DailyLimitExceeded(maxAllowedSeconds / 3600);
        }
        break;

      case EARLY_EXIT:
      case OVERTIME_TAKE:
        if (durationSeconds == null || durationSeconds <= 0) return;
        if (durationSeconds > baseRequiredSeconds) {
          throw new ApplicationValidationException.RequestedTimeExceedsScheduled(durationSeconds / 3600, baseRequiredSeconds / 3600);
        }
        break;

      default:
        break;
    }
  }
}
