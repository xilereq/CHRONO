package pl.chrono.timemanager.service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import pl.chrono.dbmanager.model.Application;
import pl.chrono.dbmanager.model.User;
import pl.chrono.dbmanager.model.enums.ApplicationStatus;
import pl.chrono.dbmanager.model.enums.ApplicationType;
import pl.chrono.dbmanager.repository.ApplicationRepository;
import pl.chrono.dbmanager.repository.UserRepository;
import pl.chrono.timemanager.exception.UserException;

@Service
@RequiredArgsConstructor
public class LeaveBalanceService {

  private static final int DEFAULT_BASE_LEAVE_DAYS = 26;
  private static final int MAX_ANNUAL_LEAVE_DAYS = 31;

  private final ApplicationRepository applicationRepository;
  private final WorkTimeCalculationService workingDaysCalculationService;
  private final UserRepository userRepository;

  public int calculateAnnualLeaveLimit(User user, int referenceYear) {
    int baseLimit = DEFAULT_BASE_LEAVE_DAYS;

    if (user.getEmploymentStartDate() == null) {
      return baseLimit;
    }

    LocalDate referenceDate = LocalDate.of(referenceYear, 12, 31);
    long yearsOfService = Math.max(0, ChronoUnit.YEARS.between(user.getEmploymentStartDate(), referenceDate));
    int totalLimit = baseLimit + (int) yearsOfService;

    return Math.min(totalLimit, MAX_ANNUAL_LEAVE_DAYS);
  }

  public int calculateAnnualLeaveLimit(User user) {
    return calculateAnnualLeaveLimit(user, LocalDate.now().getYear());
  }

  public int getUsedLeaveDaysInYear(Integer employeeId, int year) {
    User user = userRepository.findById(employeeId)
        .orElseThrow(() -> new UserException.NotFound(employeeId));

    LocalDate startOfYear = LocalDate.of(year, 1, 1);
    LocalDate endOfYear = LocalDate.of(year, 12, 31);

    List<Application> approvedLeaves =
        applicationRepository.findApplicationsByEmployeeAndTypeAndDateRange(
        employeeId, ApplicationType.HOLIDAY_LEAVE, ApplicationStatus.APPROVED, startOfYear,
        endOfYear
    );

    return approvedLeaves.stream().mapToInt(
        app -> workingDaysCalculationService.calculateWorkingDays(
            app.getStartDate(),
            app.getEndDate(), user.getPolicyForDate(app.getStartDate())
        )).sum();
  }

  public int getRolloverDaysFromPreviousYear(Integer employeeId, int currentYear) {
    int previousYear = currentYear - 1;
    User user = userRepository.findById(employeeId)
        .orElseThrow(() -> new UserException.NotFound(employeeId));

    int previousYearLimit = calculateAnnualLeaveLimit(user, previousYear);
    int usedDaysPreviousYear = getUsedLeaveDaysInYear(employeeId, previousYear);

    int rollover = previousYearLimit - usedDaysPreviousYear;

    return Math.max(0, rollover);
  }
}
