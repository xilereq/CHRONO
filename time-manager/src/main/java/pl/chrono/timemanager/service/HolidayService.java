package pl.chrono.timemanager.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.chrono.dbmanager.model.Holiday;
import pl.chrono.dbmanager.ports.I_HolidayData;
import pl.chrono.logmanager.audit.LogActivity;
import pl.chrono.timemanager.dto.request.BatchCreateHolidaysRequest;
import pl.chrono.timemanager.dto.request.CreateHolidayRequest;
import pl.chrono.timemanager.dto.request.MassAssignHolidayRequest;
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
  private final PolicyConfigurationService policyConfigurationService;

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
            holidayData.existsByDate(ext.date())
        ))
        .toList();
  }

  @Transactional
  @LogActivity(action = "IMPORT_HOLIDAYS", details = "Batch imported holidays and assigned to policies")
  public List<HolidayDto> createHolidaysBatch(BatchCreateHolidaysRequest batchRequest) {
    List<Holiday> targetHolidays = new ArrayList<>();

    for (CreateHolidayRequest req : batchRequest.holidays()) {
      List<Holiday> existing = holidayData.findAllByDate(req.date());
      if (existing.isEmpty()) {
        Holiday saved = holidayData.saveHoliday(holidayMapper.toEntity(req));
        targetHolidays.add(saved);
      } else {
        targetHolidays.addAll(existing);
      }
    }

    List<Integer> holidayIds = targetHolidays.stream()
        .map(Holiday::getHolidayId)
        .distinct()
        .toList();

    if (batchRequest.policyIds() != null && !batchRequest.policyIds().isEmpty()
        && !holidayIds.isEmpty()) {
      MassAssignHolidayRequest assignRequest = new MassAssignHolidayRequest(holidayIds);
      for (Integer policyId : batchRequest.policyIds()) {
        policyConfigurationService.assignHolidaysToPolicyBulk(policyId, assignRequest);
      }
    }

    return targetHolidays.stream()
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
    return holidayData.existsByDate(date);
  }
}
