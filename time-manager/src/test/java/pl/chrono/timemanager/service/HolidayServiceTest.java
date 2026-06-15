package pl.chrono.timemanager.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import pl.chrono.dbmanager.model.Holiday;
import pl.chrono.dbmanager.repository.HolidayPolicyRepository;
import pl.chrono.dbmanager.repository.HolidayRepository;
import pl.chrono.timemanager.dto.request.CreateHolidayRequest;
import pl.chrono.timemanager.dto.response.HolidayDto;
import pl.chrono.timemanager.exception.HolidayException;
import pl.chrono.timemanager.mapper.HolidayMapper;

@ExtendWith(MockitoExtension.class)
class HolidayServiceTest {

  @Mock
  private HolidayRepository holidayRepository;

  @Mock
  private HolidayMapper holidayMapper;

  @Mock
  private HolidayPolicyRepository holidayPolicyRepository;

  @InjectMocks
  private HolidayService holidayService;

  @Test
  void createHolidayTest() {
    CreateHolidayRequest request = new CreateHolidayRequest(
        "Nowy Rok", true, LocalDate.of(2026, 1, 1));
    Holiday mappedEntity = new Holiday();
    Holiday savedEntity = new Holiday();
    HolidayDto expectedDto = new HolidayDto(1, "Nowy Rok", true, LocalDate.of(2026, 1, 1));

    when(holidayMapper.toEntity(request)).thenReturn(mappedEntity);
    when(holidayRepository.save(mappedEntity)).thenReturn(savedEntity);
    when(holidayMapper.toDto(savedEntity)).thenReturn(expectedDto);

    HolidayDto result = holidayService.createHoliday(request);

    assertNotNull(result);
    assertEquals(expectedDto, result);

    verify(holidayMapper).toEntity(request);
    verify(holidayRepository).save(mappedEntity);
    verify(holidayMapper).toDto(savedEntity);
  }

  @Test
  void getAllHolidaysTest() {
    Holiday entity1 = new Holiday();
    Holiday entity2 = new Holiday();
    HolidayDto dto1 = new HolidayDto(1, "Święto 1", true, LocalDate.of(2026, 5, 1));
    HolidayDto dto2 = new HolidayDto(2, "Święto 2", false, LocalDate.of(2026, 5, 3));

    when(holidayRepository.findAll()).thenReturn(List.of(entity1, entity2));
    when(holidayMapper.toDto(entity1)).thenReturn(dto1);
    when(holidayMapper.toDto(entity2)).thenReturn(dto2);

    List<HolidayDto> result = holidayService.getAllHolidays();

    assertEquals(2, result.size());
    assertTrue(result.contains(dto1));
    assertTrue(result.contains(dto2));

    verify(holidayRepository).findAll();
    verify(holidayMapper, times(2)).toDto(any(Holiday.class));
  }

  @Test
  void getHolidayByIdTest() {
    Integer holidayId = 1;
    Holiday entity = new Holiday();
    HolidayDto expectedDto = new HolidayDto(
        holidayId, "Boże Narodzenie", true, LocalDate.of(2026, 12, 25));

    when(holidayRepository.findById(holidayId)).thenReturn(Optional.of(entity));
    when(holidayMapper.toDto(entity)).thenReturn(expectedDto);

    HolidayDto result = holidayService.getHolidayById(holidayId);

    assertNotNull(result);
    assertEquals(expectedDto, result);
    verify(holidayRepository).findById(holidayId);
  }

  @Test
  void getHolidayByIdExceptionTest() {
    Integer invalidId = 999;
    when(holidayRepository.findById(invalidId)).thenReturn(Optional.empty());

    assertThrows(HolidayException.NotFound.class, () -> holidayService.getHolidayById(invalidId));

    verifyNoInteractions(holidayMapper);
  }

  @Test
  void deleteHolidayTest() {
    Integer holidayId = 5;

    holidayService.deleteHoliday(holidayId);

    verify(holidayPolicyRepository).deleteByHolidayId(holidayId);
    verify(holidayRepository).deleteByIdDirectly(holidayId);
  }

  @Test
  void isHolidayTrueTest() {
    LocalDate date = LocalDate.of(2026, 5, 1);
    when(holidayRepository.existsByDateOrYearly(date)).thenReturn(true);

    boolean result = holidayService.isHoliday(date);

    assertTrue(result);
    verify(holidayRepository).existsByDateOrYearly(date);
  }

  @Test
  void isHolidayFalseTest() {
    LocalDate date = LocalDate.of(2026, 5, 2);
    when(holidayRepository.existsByDateOrYearly(date)).thenReturn(false);

    boolean result = holidayService.isHoliday(date);

    assertFalse(result);
    verify(holidayRepository).existsByDateOrYearly(date);
  }
}
