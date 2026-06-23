package pl.chrono.timemanager.service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.EnumSet;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import pl.chrono.dbmanager.model.Application;
import pl.chrono.dbmanager.model.Policy;
import pl.chrono.dbmanager.model.User;
import pl.chrono.dbmanager.model.enums.ApplicationType;
import pl.chrono.dbmanager.repository.ApplicationRepository;
import pl.chrono.timemanager.exception.WorkException;

@Service
@RequiredArgsConstructor
public class WorkTimeCalculationService {

  private final ApplicationRepository applicationRepository;

  public int calculateWorkingDays(LocalDate startDate, LocalDate endDate, Policy policy) {
    if (startDate == null || endDate == null || startDate.isAfter(endDate)) {
      return 0;
    }

    int workingDays = 0;
    LocalDate currentDate = startDate;

    while (!currentDate.isAfter(endDate)) {
      if (isWorkingDay(currentDate, policy)) {
        workingDays++;
      }
      currentDate = currentDate.plusDays(1);
    }

    return workingDays;
  }

  private boolean isWorkingDay(LocalDate date, Policy policy) {
    return getBaseRequiredSeconds(date, policy) > 0;
  }

  public int getBaseRequiredSeconds(LocalDate date, Policy policy) {
    return resolvePolicy(policy).getRequiredSecondsForDate(date);
  }

  public int getAdjustedRequiredSeconds(Integer userId, LocalDate date, Policy policy) {
    int baseRequiredSeconds = getBaseRequiredSeconds(date, policy);
    List<Application> applications = applicationRepository.findApprovedByEmployeeAndDate(
        userId, date);

    int adjustedRequiredSeconds = baseRequiredSeconds;

    for (Application app : applications) {
      int duration = app.getDurationSeconds() != null ? app.getDurationSeconds() : 0;
      switch (app.getType()) {
        case EARLY_EXIT:
        case OVERTIME_TAKE:
          adjustedRequiredSeconds -= duration;
          break;
        case OVERTIME_EARN:
          adjustedRequiredSeconds += duration;
          break;
        default:
          break;
      }
    }
    return Math.max(0, adjustedRequiredSeconds);
  }

  public boolean hasApprovedHomeOffice(Integer userId, LocalDate date) {
    return applicationRepository.findApprovedByEmployeeAndDate(userId, date).stream()
        .anyMatch(app -> app.getType() == ApplicationType.HOME_OFFICE);
  }

  public String formatSecondsToTime(long totalSeconds) {
    long hours = totalSeconds / 3600;
    long minutes = totalSeconds % 3600 / 60;
    long seconds = totalSeconds % 60;
    return String.format("%02d:%02d:%02d", hours, minutes, seconds);
  }

  public int getMaxAllowedSeconds(Policy policy) {
    return resolvePolicy(policy).getMaxAllowedSeconds();
  }

  public int getRoundingSeconds(Policy policy) {
    Policy usedPolicy = resolvePolicy(policy);
    return usedPolicy.getRoundingMinutes() != null ? usedPolicy.getRoundingMinutes() * 60 : 0;
  }

  public int getExcusedSecondsForDate(Integer userId, LocalDate date, Policy policy) {
    List<Application> applications = applicationRepository.findApprovedByEmployeeAndDate(userId, date);

    boolean hasFullDayLeave = applications.stream()
        .anyMatch(app -> app.getType() == ApplicationType.HOLIDAY_LEAVE ||
            app.getType() == ApplicationType.SICK_LEAVE);

    if (hasFullDayLeave) {
      return getBaseRequiredSeconds(date, policy);
    }

    return 0;
  }

  public Policy resolvePolicy(Policy policy) {
    return policy != null ? policy : buildDefaultPolicy();
  }

  private Policy buildDefaultPolicy() {
    Policy defaultPolicy = new Policy();
    defaultPolicy.setName("Default");
    defaultPolicy.setDailyHours(8);
    defaultPolicy.setMaxDailyHours(12);
    defaultPolicy.setMaxWeeklyHours(48);
    defaultPolicy.setRoundingMinutes(null);
    defaultPolicy.setWorkingDays(EnumSet.of(
        DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
        DayOfWeek.THURSDAY, DayOfWeek.FRIDAY
    ));
    return defaultPolicy;
  }

  public boolean isWithinEmploymentPeriod(User user, LocalDate date) {
    if (date == null) return false;
    if (user.getEmploymentStartDate() != null && date.isBefore(user.getEmploymentStartDate())) {
      return false;
    }
    return user.getEmploymentEndDate() == null || !date.isAfter(user.getEmploymentEndDate());
  }

  public void validateEmploymentPeriod(User user, LocalDate date) {
    if (!isWithinEmploymentPeriod(user, date)) {
      throw new WorkException.OutsideEmploymentPeriod();
    }
  }

  public void validateEmploymentPeriod(User user, LocalDate startDate, LocalDate endDate) {
    validateEmploymentPeriod(user, startDate);
    validateEmploymentPeriod(user, endDate);
  }
}
