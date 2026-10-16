package pl.chrono.timemanager.ports;

import java.util.List;
import pl.chrono.timemanager.dto.response.ExternalHolidayDto;

public interface I_ExternalHolidayProvider {
  List<ExternalHolidayDto> fetchHolidays(int year, String countryCode);
}
