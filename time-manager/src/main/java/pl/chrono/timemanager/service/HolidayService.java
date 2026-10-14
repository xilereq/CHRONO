package pl.chrono.timemanager.service;

import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.chrono.dbmanager.model.Holiday;
import pl.chrono.dbmanager.ports.I_HolidayData;
import pl.chrono.logmanager.audit.LogActivity;
import pl.chrono.timemanager.dto.request.CreateHolidayRequest;
import pl.chrono.timemanager.dto.response.ExternalHolidayDto;
import pl.chrono.timemanager.dto.response.HolidayDto;
import pl.chrono.timemanager.exception.HolidayException;
import pl.chrono.timemanager.mapper.HolidayMapper;
import pl.chrono.timemanager.ports.I_ExternalHolidayProvider;

@Service
@RequiredArgsConstructor
public class HolidayService {
  private final I_HolidayData holidayData;
  private final HolidayMapper holidayMapper;
  private final I_ExternalHolidayProvider externalHolidayProvider;

  @Transactional
  @LogActivity(action = "CREATE_HOLIDAY", details = "Created holiday")
  public HolidayDto createHoliday(CreateHolidayRequest request) {
    Holiday holiday = holidayMapper.toEntity(request);
    return holidayMapper.toDto(holidayData.saveHoliday(holiday));
  }

  @Transactional(readOnly = true)
  public List<ExternalHolidayDto> previewExternalHolidays(int year, String countryCode) {
    return externalHolidayProvider.fetchHolidays(year, countryCode).stream()
        .map(ext -> new ExternalHolidayDto(
            ext.name(),
            ext.originalName(),
            ext.date(),
            ext.occursYearly(),
            holidayData.existsByDateOrYearly(ext.date())
        ))
        .toList();
  }

  @Transactional
  @LogActivity(action = "IMPORT_HOLIDAYS", details = "Batch imported holidays")
  public List<HolidayDto> createHolidaysBatch(List<CreateHolidayRequest> requests) {
    List<Holiday> toSave = requests.stream()
        .filter(req -> !holidayData.existsByDateOrYearly(req.date()))
        .map(holidayMapper::toEntity)
        .toList();

    return holidayData.saveAllHolidays(toSave).stream()
        .map(holidayMapper::toDto)
        .toList();
  }

  @Transactional(readOnly = true)
  public List<HolidayDto> getAllHolidays() {
    return holidayData.findAllHolidays().stream().map(holidayMapper::toDto).toList();
  }

  @Transactional(readOnly = true)
  public HolidayDto getHolidayById(Integer holidayId) {
    return holidayData.findHolidayById(holidayId).map(holidayMapper::toDto)
        .orElseThrow(() -> new HolidayException.NotFound(holidayId));
  }

  @Transactional
  @LogActivity(action = "DELETE_HOLIDAY", details = "Deleted holiday")
  public void deleteHoliday(Integer holidayId) {
    holidayData.deleteHolidayById(holidayId);
  }

  @Transactional(readOnly = true)
  public boolean isHoliday(LocalDate date) {
    return holidayData.existsByDateOrYearly(date);
  }
}
