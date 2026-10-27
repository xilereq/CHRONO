package pl.chrono.timemanager.service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import pl.chrono.dbmanager.model.Holiday;
import pl.chrono.dbmanager.model.HolidayPolicy;
import pl.chrono.dbmanager.model.Policy;
import pl.chrono.dbmanager.ports.I_HolidayData;
import pl.chrono.dbmanager.ports.I_PolicyData;
import pl.chrono.timemanager.dto.request.AssignHolidayRequest;
import pl.chrono.timemanager.dto.request.CreatePolicyRequest;
import pl.chrono.timemanager.dto.request.MassAssignHolidayRequest;
import pl.chrono.timemanager.dto.response.HolidayDto;
import pl.chrono.timemanager.dto.response.PolicyDto;
import pl.chrono.timemanager.exception.HolidayException;
import pl.chrono.timemanager.exception.PolicyException;
import pl.chrono.timemanager.mapper.HolidayMapper;
import pl.chrono.timemanager.mapper.PolicyMapper;


@ExtendWith(MockitoExtension.class)
public class PolicyConfigurationServiceTest {

  @Mock
  private I_PolicyData policyData;

  @Mock
  private PolicyMapper policyMapper;

  @Mock
  private I_HolidayData holidayData;

  @Mock
  private HolidayMapper holidayMapper;

  @InjectMocks
  private PolicyConfigurationService policyConfigurationService;

  @Test
  void createPolicyTest() {
    CreatePolicyRequest request = new CreatePolicyRequest(
        "Test Policy", 8, 10, 40, 15,
        List.of("Monday", "Tuesday", "Wednesday", "Thursday", "Friday")
    );

    Policy mappedPolicy = new Policy();
    Policy savedPolicy = new Policy();

    List<String> days = List.of("MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY");
    Set<DayOfWeek> dayOfWeekSet = days.stream().map(DayOfWeek::valueOf).collect(Collectors.toSet());

    PolicyDto expectedDto = new PolicyDto(1, "Test Policy", 8, 10, 40, 15, dayOfWeekSet);

    when(policyMapper.toEntity(request)).thenReturn(mappedPolicy);
    when(policyData.savePolicy(mappedPolicy)).thenReturn(savedPolicy);
    when(policyMapper.toDto(savedPolicy)).thenReturn(expectedDto);

    PolicyDto result = policyConfigurationService.createPolicy(request);

    assertNotNull(result);
    assertEquals(expectedDto, result);

    verify(policyMapper).toEntity(request);
    verify(policyData).savePolicy(mappedPolicy);
    verify(policyMapper).toDto(savedPolicy);
  }

  @Test
  void getAllPoliciesTest() {
    Policy policy1 = new Policy();
    PolicyDto dto1 = new PolicyDto(1, "Polityka 1", 8, 10, 40, 15, Set.of(DayOfWeek.MONDAY));

    when(policyData.findAllPolicies()).thenReturn(List.of(policy1));
    when(policyMapper.toDto(policy1)).thenReturn(dto1);

    List<PolicyDto> result = policyConfigurationService.getAllPolicies();

    assertEquals(1, result.size());
    assertEquals(dto1, result.getFirst());
  }

  @Test
  void getPolicyByIdTest() {
    Integer policyId = 1;
    Policy policy = new Policy();
    PolicyDto dto = new PolicyDto(policyId, "Test", 8, 10, 40, 15, Set.of(DayOfWeek.MONDAY));

    when(policyData.findPolicyById(policyId)).thenReturn(Optional.of(policy));
    when(policyMapper.toDto(policy)).thenReturn(dto);

    PolicyDto result = policyConfigurationService.getPolicyById(policyId);

    assertEquals(dto, result);
  }

  @Test
  void getPolicyByIdExceptionTest() {
    Integer policyId = 999;
    when(policyData.findPolicyById(policyId)).thenReturn(Optional.empty());

    assertThrows(
        PolicyException.NotFound.class, () -> policyConfigurationService.getPolicyById(policyId));
  }

  @Test
  void getHolidaysForPolicyTest() {
    Integer policyId = 1;
    Policy policy = new Policy();
    Holiday holiday = new Holiday();

    HolidayPolicy holidayPolicy = new HolidayPolicy();
    holidayPolicy.setHoliday(holiday);

    HolidayDto holidayDto = new HolidayDto(1, "Majówka", LocalDate.of(2026, 5, 1));

    when(policyData.findPolicyById(policyId)).thenReturn(Optional.of(policy));
    when(policyData.findHolidayPoliciesByPolicy(policy)).thenReturn(List.of(holidayPolicy));
    when(holidayMapper.toDto(holiday)).thenReturn(holidayDto);

    List<HolidayDto> result = policyConfigurationService.getHolidaysForPolicy(policyId);

    assertEquals(1, result.size());
    assertEquals(holidayDto, result.getFirst());
  }

  @Test
  void getHolidaysForPolicyExceptionTest() {
    Integer policyId = 999;
    when(policyData.findPolicyById(policyId)).thenReturn(Optional.empty());

    assertThrows(
        PolicyException.NotFound.class,
        () -> policyConfigurationService.getHolidaysForPolicy(policyId)
    );
  }

  @Test
  void assignHolidayToPolicyTest() {
    Integer policyId = 1;
    Integer holidayId = 2;
    AssignHolidayRequest request = new AssignHolidayRequest(holidayId);

    Policy policy = new Policy();
    Holiday holiday = new Holiday();

    when(policyData.findPolicyById(policyId)).thenReturn(Optional.of(policy));
    when(holidayData.findHolidayById(holidayId)).thenReturn(Optional.of(holiday));
    when(policyData.existsHolidayPolicy(policy, holiday)).thenReturn(false);

    policyConfigurationService.assignHolidayToPolicy(policyId, request);

    verify(policyData).saveHolidayPolicy(any(HolidayPolicy.class));
  }

  @Test
  void assignHolidayToPolicyExceptionTest() {
    Integer policyId = 1;

    when(policyData.findPolicyById(policyId)).thenReturn(Optional.empty());

    assertThrows(
        PolicyException.NotFound.class,
        () -> policyConfigurationService.assignHolidayToPolicy(
            policyId,
            new AssignHolidayRequest(2)
        )
    );
  }

  @Test
  void assignHolidayToPolicyExceptionTest2() {
    Integer policyId = 1;
    AssignHolidayRequest request = new AssignHolidayRequest(2);
    Policy policy = new Policy();
    Holiday holiday = new Holiday();

    when(policyData.findPolicyById(policyId)).thenReturn(Optional.of(policy));
    when(holidayData.findHolidayById(request.holidayId())).thenReturn(Optional.of(holiday));

    when(policyData.existsHolidayPolicy(policy, holiday)).thenReturn(true);

    assertThrows(
        HolidayException.AlreadyAssigned.class,
        () -> policyConfigurationService.assignHolidayToPolicy(policyId, request)
    );

    verify(policyData, never()).saveHolidayPolicy(any());
  }

  @Test
  void assignHolidayToPolicyExceptionTest3() {
    Integer policyId = 1;
    AssignHolidayRequest request = new AssignHolidayRequest(999);
    Policy policy = new Policy();

    when(policyData.findPolicyById(policyId)).thenReturn(Optional.of(policy));
    when(holidayData.findHolidayById(request.holidayId())).thenReturn(Optional.empty());

    assertThrows(
        HolidayException.NotFound.class,
        () -> policyConfigurationService.assignHolidayToPolicy(policyId, request)
    );
  }

  @Test
  void removeHolidayFromPolicyTest() {
    Integer policyId = 1;
    Integer holidayId = 2;
    Policy policy = new Policy();
    Holiday holiday = new Holiday();
    HolidayPolicy assignment = new HolidayPolicy();

    when(policyData.findPolicyById(policyId)).thenReturn(Optional.of(policy));
    when(holidayData.findHolidayById(holidayId)).thenReturn(Optional.of(holiday));
    when(policyData.findHolidayPolicyByPolicyAndHoliday(policy, holiday)).thenReturn(
        Optional.of(assignment));

    policyConfigurationService.removeHolidayFromPolicy(policyId, holidayId);

    verify(policyData).deleteHolidayPolicy(assignment);
  }

  @Test
  void removeHolidayFromPolicyExceptionTest() {
    Integer policyId = 1;

    when(policyData.findPolicyById(policyId)).thenReturn(Optional.empty());

    assertThrows(
        PolicyException.NotFound.class,
        () -> policyConfigurationService.removeHolidayFromPolicy(policyId, 2)
    );
  }

  @Test
  void removeHolidayFromPolicyExceptionTest2() {
    Integer policyId = 1;
    Integer holidayId = 2;

    Policy policy = new Policy();

    when(policyData.findPolicyById(policyId)).thenReturn(Optional.of(policy));
    when(holidayData.findHolidayById(holidayId)).thenReturn(Optional.empty());

    assertThrows(
        HolidayException.NotFound.class,
        () -> policyConfigurationService.removeHolidayFromPolicy(policyId, holidayId)
    );
  }


  @Test
  void removeHolidayFromPolicyExceptionTest3() {
    Integer policyId = 1;
    Integer holidayId = 2;
    Policy policy = new Policy();
    Holiday holiday = new Holiday();

    when(policyData.findPolicyById(policyId)).thenReturn(Optional.of(policy));
    when(holidayData.findHolidayById(holidayId)).thenReturn(Optional.of(holiday));

    when(policyData.findHolidayPolicyByPolicyAndHoliday(policy, holiday)).thenReturn(
        Optional.empty());

    assertThrows(
        HolidayException.NotAssigned.class,
        () -> policyConfigurationService.removeHolidayFromPolicy(policyId, holidayId)
    );
  }

  @Test
  void deletePolicyTest() {
    Integer policyId = 1;
    Policy policy = new Policy();

    when(policyData.findPolicyById(policyId)).thenReturn(Optional.of(policy));

    policyConfigurationService.deletePolicy(policyId);

    verify(policyData).deleteHolidayPoliciesByPolicy(policy);
    verify(policyData).deletePolicy(policy);
  }

  @Test
  void deletePolicyExceptionTest() {
    Integer policyId = 1;

    when(policyData.findPolicyById(policyId)).thenReturn(Optional.empty());

    assertThrows(
        PolicyException.NotFound.class,
        () -> policyConfigurationService.deletePolicy(policyId)
    );
  }

  @Test
  void assignHolidaysToPolicyPolicyNotFoundExceptionTest() {
    Integer policyId = 999;
    MassAssignHolidayRequest request = new MassAssignHolidayRequest(List.of(1, 2));

    when(policyData.findPolicyById(policyId)).thenReturn(Optional.empty());

    assertThrows(
        PolicyException.NotFound.class,
        () -> policyConfigurationService.assignHolidaysToPolicyBulk(policyId, request)
    );
  }

  @Test
  void assignHolidaysToPolicyBulkNoneFoundExceptionTest() {
    Integer policyId = 1;
    MassAssignHolidayRequest request = new MassAssignHolidayRequest(List.of(99, 100));
    Policy policy = new Policy();

    when(policyData.findPolicyById(policyId)).thenReturn(Optional.of(policy));
    when(holidayData.findHolidaysByIds(request.holidayIds())).thenReturn(List.of());

    assertThrows(
        HolidayException.NoneFound.class,
        () -> policyConfigurationService.assignHolidaysToPolicyBulk(policyId, request)
    );
  }

  @Test
  void assignHolidaysToPolicyAllAlreadyAssignedTest() {
    Integer policyId = 1;
    MassAssignHolidayRequest request = new MassAssignHolidayRequest(List.of(1));
    Policy policy = new Policy();

    Holiday holiday = new Holiday();
    holiday.setHolidayId(1);

    HolidayPolicy existingHp = new HolidayPolicy();
    existingHp.setHoliday(holiday);

    when(policyData.findPolicyById(policyId)).thenReturn(Optional.of(policy));
    when(holidayData.findHolidaysByIds(request.holidayIds())).thenReturn(List.of(holiday));

    when(policyData.findHolidayPoliciesByPolicy(policy)).thenReturn(List.of(existingHp));

    policyConfigurationService.assignHolidaysToPolicyBulk(policyId, request);

    verify(policyData, never()).saveAllHolidayPolicies(any());
  }

  @Test
  void assignHolidaysToPolicyMixedAssignmentsTest() {
    Integer policyId = 1;
    MassAssignHolidayRequest request = new MassAssignHolidayRequest(List.of(1, 2));
    Policy policy = new Policy();

    Holiday assignedHoliday = new Holiday();
    assignedHoliday.setHolidayId(1);

    Holiday newHoliday = new Holiday();
    newHoliday.setHolidayId(2);

    HolidayPolicy existingHp = new HolidayPolicy();
    existingHp.setHoliday(assignedHoliday);

    when(policyData.findPolicyById(policyId)).thenReturn(Optional.of(policy));

    when(holidayData.findHolidaysByIds(request.holidayIds())).thenReturn(
        List.of(assignedHoliday, newHoliday));

    when(policyData.findHolidayPoliciesByPolicy(policy)).thenReturn(List.of(existingHp));

    policyConfigurationService.assignHolidaysToPolicyBulk(policyId, request);

    ArgumentCaptor<List<HolidayPolicy>> captor = ArgumentCaptor.forClass(List.class);
    verify(policyData).saveAllHolidayPolicies(captor.capture());

    List<HolidayPolicy> savedList = captor.getValue();

    assertEquals(1, savedList.size());
    assertEquals(2, savedList.getFirst().getHoliday().getHolidayId());
  }
}
