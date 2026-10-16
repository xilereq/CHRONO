package pl.chrono.dbmanager.ports;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import pl.chrono.dbmanager.model.Holiday;

public interface I_HolidayData {
  Holiday saveHoliday(Holiday holiday);

  List<Holiday> findAllHolidays();

  Optional<Holiday> findHolidayById(Integer holidayId);

  Optional<Holiday> findByNameAndDate(String name, LocalDate date);

  void deleteHolidayById(Integer holidayId);

  boolean existsByDate(LocalDate date);

  boolean existsByNameAndDate(String name, LocalDate date);
}
