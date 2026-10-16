package pl.chrono.timemanager.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
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

  private static final int MIN_SUPPORTED_YEAR = 1976;
  private static final int MAX_SUPPORTED_YEAR = 2076;
  private static final Set<String> ISO_COUNTRIES = Set.of(Locale.getISOCountries());

  private final I_HolidayData holidayData;
  private final HolidayMapper holidayMapper;
  private final I_ExternalHolidayProvider externalHolidayProvider;
  private final PolicyConfigurationService policyConfigurationService;

  @Transactional
  @LogActivity(action = "CREATE_HOLIDAY", details = "Created holiday")
  public HolidayDto createHoliday(CreateHolidayRequest request) {
    String cleanName = request.name().trim();
    if (holidayData.existsByNameAndDate(cleanName, request.date())) {
      throw new HolidayException.AlreadyExists(cleanName, request.date());
    }

    Holiday holiday = holidayMapper.toEntity(new CreateHolidayRequest(cleanName, request.date()));
    return holidayMapper.toDto(holidayData.saveHoliday(holiday));
  }

  @Transactional(readOnly = true)
  public List<ExternalHolidayDto> previewExternalHolidays(int year, String countryCode) {
    if (year < MIN_SUPPORTED_YEAR || year > MAX_SUPPORTED_YEAR) {
      throw new HolidayException.InvalidYear(year);
    }

    if (countryCode == null || countryCode.isBlank()) {
      throw new HolidayException.InvalidCountryCode(String.valueOf(countryCode));
    }

    String normalizedCode = countryCode.trim().toUpperCase(Locale.ROOT);
    if (!ISO_COUNTRIES.contains(normalizedCode)) {
      throw new HolidayException.InvalidCountryCode(countryCode);
    }

    return externalHolidayProvider.fetchHolidays(year, normalizedCode).stream()
        .map(ext -> new ExternalHolidayDto(
            ext.name(),
            ext.originalName(),
            ext.date(),
            holidayData.existsByNameAndDate(ext.name().trim(), ext.date())
        ))
        .toList();
  }

  @Transactional
  @LogActivity(action = "IMPORT_HOLIDAYS", details = "Batch imported holidays and assigned to policies")
  public List<HolidayDto> createHolidaysBatch(BatchCreateHolidaysRequest batchRequest) {
    Map<String, CreateHolidayRequest> uniqueRequests = new LinkedHashMap<>();
    for (CreateHolidayRequest req : batchRequest.holidays()) {
      String cleanName = req.name().trim();
      String key = cleanName.toLowerCase(Locale.ROOT) + "|" + req.date();
      uniqueRequests.putIfAbsent(key, new CreateHolidayRequest(cleanName, req.date()));
    }

    List<Holiday> targetHolidays = new ArrayList<>();
    for (CreateHolidayRequest req : uniqueRequests.values()) {
      Holiday holiday = holidayData.findByNameAndDate(req.name(), req.date())
          .orElseGet(() -> holidayData.saveHoliday(holidayMapper.toEntity(req)));
      targetHolidays.add(holiday);
    }

    List<Integer> holidayIds = targetHolidays.stream()
        .map(Holiday::getHolidayId)
        .distinct()
        .toList();

    if (batchRequest.policyIds() != null && !batchRequest.policyIds().isEmpty()
        && !holidayIds.isEmpty()) {
      for (Integer policyId : batchRequest.policyIds().stream().distinct().toList()) {
        Set<Integer> alreadyAssignedIds = policyConfigurationService.getHolidaysForPolicy(policyId)
            .stream()
            .map(HolidayDto::holidayId)
            .collect(Collectors.toSet());

        List<Integer> unassignedHolidayIds = holidayIds.stream()
            .filter(id -> !alreadyAssignedIds.contains(id))
            .toList();

        if (!unassignedHolidayIds.isEmpty()) {
          policyConfigurationService.assignHolidaysToPolicyBulk(
              policyId, new MassAssignHolidayRequest(unassignedHolidayIds));
        }
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
