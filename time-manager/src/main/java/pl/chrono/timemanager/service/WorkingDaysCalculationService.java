package pl.chrono.timemanager.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class WorkingDaysCalculationService {

  private final HolidayService holidayService;

  public int calculateWorkingDays(LocalDate startDate, LocalDate endDate) {
    if (startDate == null || endDate == null || startDate.isAfter(endDate)) {
      return 0;
    }

    int workingDays = 0;
    LocalDate currentDate = startDate;

    while (!currentDate.isAfter(endDate)) {
      if (isWorkingDay(currentDate)) {
        workingDays++;
      }
      currentDate = currentDate.plusDays(1);
    }

    return workingDays;
  }

  private boolean isWorkingDay(LocalDate date) {
    DayOfWeek dayOfWeek = date.getDayOfWeek();

    if (dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY) {
      return false;
    }

    if (holidayService.isHoliday(date)) {
      return false;
    }

    return true;
  }
}
