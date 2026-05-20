package pl.chrono.timemanager.service;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.chrono.dbmanager.model.Holiday;
import pl.chrono.dbmanager.repository.HolidayPolicyRepository;
import pl.chrono.dbmanager.repository.HolidayRepository;
import pl.chrono.timemanager.dto.request.CreateHolidayRequest;
import pl.chrono.timemanager.dto.response.HolidayDto;
import pl.chrono.timemanager.exception.HolidayException;
import pl.chrono.timemanager.mapper.HolidayMapper;

@Service
@RequiredArgsConstructor
public class HolidayService {
  private final HolidayRepository holidayRepository;
  private final HolidayMapper holidayMapper;
  private final HolidayPolicyRepository holidayPolicyRepository;

  @Transactional
  public HolidayDto createHoliday(CreateHolidayRequest request) {
    Holiday holiday = holidayMapper.toEntity(request);
    return holidayMapper.toDto(holidayRepository.save(holiday));
  }

  @Transactional(readOnly = true)
  public List<HolidayDto> getAllHolidays() {
    return holidayRepository.findAll().stream().map(holidayMapper::toDto).toList();
  }

  @Transactional(readOnly = true)
  public HolidayDto getHolidayById(Integer holidayId) {
    return holidayRepository.findById(holidayId).map(holidayMapper::toDto)
        .orElseThrow(() -> new HolidayException.NotFound(holidayId));
  }

  @Transactional
  public void deleteHoliday(Integer holidayId) {
    Holiday holiday = holidayRepository.findById(holidayId)
        .orElseThrow(() -> new HolidayException.NotFound(holidayId));

    holidayPolicyRepository.deleteByHoliday(holiday);
    holidayRepository.delete(holiday);
  }
}


