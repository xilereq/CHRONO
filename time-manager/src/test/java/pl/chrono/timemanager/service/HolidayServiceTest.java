package pl.chrono.timemanager.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pl.chrono.dbmanager.model.Holiday;
import pl.chrono.dbmanager.ports.I_HolidayData;
import pl.chrono.timemanager.dto.request.BatchCreateHolidaysRequest;
import pl.chrono.timemanager.dto.request.CreateHolidayRequest;
import pl.chrono.timemanager.dto.request.MassAssignHolidayRequest;
import pl.chrono.timemanager.dto.response.ExternalHolidayDto;
import pl.chrono.timemanager.dto.response.HolidayDto;
import pl.chrono.timemanager.exception.HolidayException;
import pl.chrono.timemanager.mapper.HolidayMapper;
import pl.chrono.timemanager.ports.I_ExternalHolidayProvider;

@ExtendWith(MockitoExtension.class)
class HolidayServiceTest {

  @Mock
  private I_HolidayData holidayData;

  @Mock
  private HolidayMapper holidayMapper;

  @Mock
  private I_ExternalHolidayProvider externalHolidayProvider;

  @Mock
  private PolicyConfigurationService policyConfigurationService;

  @InjectMocks
  private HolidayService holidayService;

  @Test
  void createHolidayTest() {
    CreateHolidayRequest request = new CreateHolidayRequest(
        "Nowy Rok", LocalDate.of(2026, 1, 1));
    Holiday mappedEntity = new Holiday();
    Holiday savedEntity = new Holiday();
    HolidayDto expectedDto = new HolidayDto(1, "Nowy Rok", LocalDate.of(2026, 1, 1));

    when(holidayMapper.toEntity(request)).thenReturn(mappedEntity);
    when(holidayData.saveHoliday(mappedEntity)).thenReturn(savedEntity);
    when(holidayMapper.toDto(savedEntity)).thenReturn(expectedDto);

    HolidayDto result = holidayService.createHoliday(request);

    assertNotNull(result);
    assertEquals(expectedDto, result);

    verify(holidayMapper).toEntity(request);
    verify(holidayData).saveHoliday(mappedEntity);
    verify(holidayMapper).toDto(savedEntity);
  }

  @Test
  void previewExternalHolidaysTest() {
    LocalDate date1 = LocalDate.of(2026, 1, 1);
    LocalDate date2 = LocalDate.of(2026, 1, 6);

    List<ExternalHolidayDto> externalList = List.of(
        new ExternalHolidayDto("Nowy Rok", "New Year's Day", date1, false),
        new ExternalHolidayDto("Trzech Króli", "Epiphany", date2, false)
    );

    when(externalHolidayProvider.fetchHolidays(2026, "PL")).thenReturn(externalList);
    when(holidayData.existsByNameAndDate("Nowy Rok", date1)).thenReturn(true);
    when(holidayData.existsByNameAndDate("Trzech Króli", date2)).thenReturn(false);

    List<ExternalHolidayDto> result = holidayService.previewExternalHolidays(2026, "PL");

    assertEquals(2, result.size());
    assertTrue(result.get(0).alreadyExists());
    assertFalse(result.get(1).alreadyExists());
  }

  @Test
  void createHolidaysBatchTest() {
    LocalDate date1 = LocalDate.of(2026, 1, 1);
    LocalDate date2 = LocalDate.of(2026, 5, 1);

    CreateHolidayRequest req1 = new CreateHolidayRequest("Nowy Rok", date1);
    CreateHolidayRequest req2 = new CreateHolidayRequest("Święto Pracy", date2);
    BatchCreateHolidaysRequest batchRequest = new BatchCreateHolidaysRequest(
        List.of(req1, req2), List.of(1)
    );

    Holiday existingHoliday = new Holiday();
    existingHoliday.setHolidayId(10);
    existingHoliday.setName("Nowy Rok");
    existingHoliday.setDate(date1);

    Holiday newHoliday = new Holiday();
    newHoliday.setHolidayId(20);
    newHoliday.setName("Święto Pracy");
    newHoliday.setDate(date2);

    when(holidayData.findByNameAndDate("Nowy Rok", date1)).thenReturn(Optional.of(existingHoliday));
    when(holidayData.findByNameAndDate("Święto Pracy", date2)).thenReturn(Optional.empty());
    when(policyConfigurationService.getHolidaysForPolicy(1)).thenReturn(List.of());
    when(holidayMapper.toEntity(req2)).thenReturn(newHoliday);
    when(holidayData.saveHoliday(newHoliday)).thenReturn(newHoliday);

    when(holidayMapper.toDto(existingHoliday)).thenReturn(new HolidayDto(10, "Nowy Rok", date1));
    when(holidayMapper.toDto(newHoliday)).thenReturn(new HolidayDto(20, "Święto Pracy", date2));

    List<HolidayDto> result = holidayService.createHolidaysBatch(batchRequest);

    assertEquals(2, result.size());
    verify(holidayData, never()).saveHoliday(existingHoliday);
    verify(holidayData).saveHoliday(newHoliday);

    MassAssignHolidayRequest expectedAssign = new MassAssignHolidayRequest(List.of(10, 20));
    verify(policyConfigurationService).assignHolidaysToPolicyBulk(eq(1), eq(expectedAssign));
  }

  @Test
  void getAllHolidaysTest() {
    Holiday entity1 = new Holiday();
    Holiday entity2 = new Holiday();
    HolidayDto dto1 = new HolidayDto(1, "Święto 1", LocalDate.of(2026, 5, 1));
    HolidayDto dto2 = new HolidayDto(2, "Święto 2", LocalDate.of(2026, 5, 3));

    when(holidayData.findAllHolidays()).thenReturn(List.of(entity1, entity2));
    when(holidayMapper.toDto(entity1)).thenReturn(dto1);
    when(holidayMapper.toDto(entity2)).thenReturn(dto2);

    List<HolidayDto> result = holidayService.getAllHolidays();

    assertEquals(2, result.size());
    assertTrue(result.contains(dto1));
    assertTrue(result.contains(dto2));

    verify(holidayData).findAllHolidays();
    verify(holidayMapper, times(2)).toDto(any(Holiday.class));
  }

  @Test
  void getHolidayByIdTest() {
    Integer holidayId = 1;
    Holiday entity = new Holiday();
    HolidayDto expectedDto = new HolidayDto(
        holidayId, "Boże Narodzenie", LocalDate.of(2026, 12, 25));

    when(holidayData.findHolidayById(holidayId)).thenReturn(Optional.of(entity));
    when(holidayMapper.toDto(entity)).thenReturn(expectedDto);

    HolidayDto result = holidayService.getHolidayById(holidayId);

    assertNotNull(result);
    assertEquals(expectedDto, result);
    verify(holidayData).findHolidayById(holidayId);
  }

  @Test
  void getHolidayByIdExceptionTest() {
    Integer invalidId = 999;
    when(holidayData.findHolidayById(invalidId)).thenReturn(Optional.empty());

    assertThrows(HolidayException.NotFound.class, () -> holidayService.getHolidayById(invalidId));

    verifyNoInteractions(holidayMapper);
  }

  @Test
  void deleteHolidayTest() {
    Integer holidayId = 5;

    holidayService.deleteHoliday(holidayId);

    verify(holidayData).deleteHolidayById(holidayId);
  }

  @Test
  void isHolidayTrueTest() {
    LocalDate date = LocalDate.of(2026, 5, 1);
    when(holidayData.existsByDate(date)).thenReturn(true);

    boolean result = holidayService.isHoliday(date);

    assertTrue(result);
    verify(holidayData).existsByDate(date);
  }

  @Test
  void isHolidayFalseTest() {
    LocalDate date = LocalDate.of(2026, 5, 2);
    when(holidayData.existsByDate(date)).thenReturn(false);

    boolean result = holidayService.isHoliday(date);

    assertFalse(result);
    verify(holidayData).existsByDate(date);
  }
}
