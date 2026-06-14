package pl.chrono.timemanager.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import pl.chrono.dbmanager.model.Application;
import pl.chrono.dbmanager.model.Policy;
import pl.chrono.dbmanager.model.enums.ApplicationType;
import pl.chrono.dbmanager.repository.ApplicationRepository;
import pl.chrono.dbmanager.repository.HolidayRepository;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class WorkTimeCalculationService {

  private final HolidayService holidayService;
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
    if (policy != null) {
      return policy.getRequiredSecondsForDate(date);
    }

    DayOfWeek dayOfWeek = date.getDayOfWeek();
    if (dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY || holidayService.isHoliday(
        date)) {
      return 0;
    }
    return 8 * 3600;
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
    if (policy != null && policy.getMaxDailyHours() != null) {
      return policy.getMaxDailyHours() * 3600;
    }
    return 12 * 3600;
  }

  public int getRoundingSeconds(Policy policy) {
    if (policy != null && policy.getRoundingMinutes() != null) {
      return policy.getRoundingMinutes() * 60;
    }
    return 0;
  }
}
