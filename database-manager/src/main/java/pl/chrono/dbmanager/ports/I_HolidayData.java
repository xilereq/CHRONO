package pl.chrono.dbmanager.ports;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import pl.chrono.dbmanager.model.Holiday;

public interface I_HolidayData {
  Holiday saveHoliday(Holiday holiday);

  List<Holiday> saveAllHolidays(List<Holiday> holidays);

  List<Holiday> findAllHolidays();

  Optional<Holiday> findHolidayById(Integer holidayId);

  void deleteHolidayById(Integer holidayId);

  boolean existsByDateOrYearly(LocalDate date);
}
