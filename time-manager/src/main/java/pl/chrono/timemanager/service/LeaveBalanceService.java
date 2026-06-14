package pl.chrono.timemanager.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import pl.chrono.dbmanager.model.Application;
import pl.chrono.dbmanager.model.User;
import pl.chrono.dbmanager.model.enums.ApplicationStatus;
import pl.chrono.dbmanager.model.enums.ApplicationType;
import pl.chrono.dbmanager.repository.ApplicationRepository;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@RequiredArgsConstructor
public class LeaveBalanceService {

  private static final int DEFAULT_BASE_LEAVE_DAYS = 26;
  private static final int MAX_ANNUAL_LEAVE_DAYS = 31;

  private final ApplicationRepository applicationRepository;
  private final WorkingDaysCalculationService workingDaysCalculationService;

  public int calculateAnnualLeaveLimit(User user) {
    int baseLimit = DEFAULT_BASE_LEAVE_DAYS;

    if (user.getEmploymentStartDate() == null) {
      return baseLimit;
    }

    long yearsOfService = ChronoUnit.YEARS.between(user.getEmploymentStartDate(), LocalDate.now());
    int totalLimit = baseLimit + (int) yearsOfService;

    return Math.min(totalLimit, MAX_ANNUAL_LEAVE_DAYS);
  }

  public int getUsedLeaveDaysInYear(Integer employeeId, int year) {
    LocalDate startOfYear = LocalDate.of(year, 1, 1);
    LocalDate endOfYear = LocalDate.of(year, 12, 31);

    List<Application> approvedLeaves = applicationRepository.findApplicationsByEmployeeAndTypeAndDateRange(
        employeeId,
        ApplicationType.HOLIDAY_LEAVE,
        ApplicationStatus.APPROVED,
        startOfYear,
        endOfYear
    );

    return approvedLeaves.stream()
        .mapToInt(app -> workingDaysCalculationService.calculateWorkingDays(app.getStartDate(), app.getEndDate()))
        .sum();
  }

  public int getRolloverDaysFromPreviousYear(Integer employeeId, int currentYear) {
    int previousYear = currentYear - 1;
    int usedDaysPreviousYear = getUsedLeaveDaysInYear(employeeId, previousYear);

    int rollover = DEFAULT_BASE_LEAVE_DAYS - usedDaysPreviousYear;

    return Math.max(0, rollover);
  }
}
