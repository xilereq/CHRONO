package pl.chrono.dbmanager.adapter;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import pl.chrono.dbmanager.model.Holiday;
import pl.chrono.dbmanager.ports.I_HolidayData;
import pl.chrono.dbmanager.repository.HolidayPolicyRepository;
import pl.chrono.dbmanager.repository.HolidayRepository;

@Component
@RequiredArgsConstructor
public class HolidayDataAdapter implements I_HolidayData {
  private final HolidayRepository holidayRepository;
  private final HolidayPolicyRepository holidayPolicyRepository;

  @Override
  public Holiday saveHoliday(Holiday holiday) {
    return holidayRepository.save(holiday);
  }

  @Override
  public List<Holiday> saveAllHolidays(List<Holiday> holidays) {
    return holidayRepository.saveAll(holidays);
  }

  @Override
  public List<Holiday> findAllHolidays() {
    return holidayRepository.findAll();
  }

  @Override
  public Optional<Holiday> findHolidayById(Integer holidayId) {
    return holidayRepository.findById(holidayId);
  }

  @Override
  public void deleteHolidayById(Integer holidayId) {
    holidayPolicyRepository.deleteByHolidayId(holidayId);
    holidayRepository.deleteByIdDirectly(holidayId);
  }

  @Override
  public boolean existsByDateOrYearly(LocalDate date) {
    return holidayRepository.existsByDateOrYearly(date, date.getMonthValue(), date.getDayOfMonth());
  }
}
