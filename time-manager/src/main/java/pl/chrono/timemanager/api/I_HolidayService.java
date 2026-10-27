package pl.chrono.timemanager.api;

import java.util.List;
import pl.chrono.timemanager.dto.request.BatchCreateHolidaysRequest;
import pl.chrono.timemanager.dto.request.CreateHolidayRequest;
import pl.chrono.timemanager.dto.response.ExternalHolidayDto;
import pl.chrono.timemanager.dto.response.HolidayDto;

public interface I_HolidayService {
  HolidayDto createHoliday(CreateHolidayRequest request);

  List<ExternalHolidayDto> previewExternalHolidays(int year, String countryCode);

  List<HolidayDto> createHolidaysBatch(BatchCreateHolidaysRequest batchRequest);

  List<HolidayDto> getAllHolidays();

  HolidayDto getHolidayById(Integer holidayId);

  void deleteHoliday(Integer holidayId);
}
