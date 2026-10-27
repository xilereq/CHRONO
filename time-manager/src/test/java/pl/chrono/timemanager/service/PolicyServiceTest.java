package pl.chrono.timemanager.service;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pl.chrono.timemanager.dto.request.AssignHolidayRequest;
import pl.chrono.timemanager.dto.request.AssignPolicyRequest;
import pl.chrono.timemanager.dto.request.CreatePolicyRequest;
import pl.chrono.timemanager.dto.request.MassAssignHolidayRequest;
import pl.chrono.timemanager.dto.request.MassAssignPolicyRequest;
import pl.chrono.timemanager.dto.response.HolidayDto;
import pl.chrono.timemanager.dto.response.PolicyDto;

@ExtendWith(MockitoExtension.class)
class PolicyServiceTest {

  @Mock
  private PolicyConfigurationService configurationService;

  @Mock
  private PolicyManagementService managementService;

  @InjectMocks
  private PolicyService policyService;

  private static PolicyDto samplePolicyDto() {
    return new PolicyDto(1, "Standard", 8, 12, 48, 0, Set.of(DayOfWeek.MONDAY));
  }

  @Test
  void createPolicyDelegatesToConfigurationServiceTest() {
    CreatePolicyRequest request = new CreatePolicyRequest(
        "Standard", 8, 12, 48, 0, List.of("MONDAY"));
    PolicyDto expected = samplePolicyDto();
    when(configurationService.createPolicy(request)).thenReturn(expected);

    PolicyDto result = policyService.createPolicy(request);

    assertSame(expected, result);
    verify(configurationService).createPolicy(request);
    verifyNoMoreInteractions(configurationService, managementService);
  }

  @Test
  void getAllPoliciesDelegatesToConfigurationServiceTest() {
    List<PolicyDto> expected = List.of(samplePolicyDto());
    when(configurationService.getAllPolicies()).thenReturn(expected);

    List<PolicyDto> result = policyService.getAllPolicies();

    assertSame(expected, result);
    verify(configurationService).getAllPolicies();
    verifyNoMoreInteractions(configurationService, managementService);
  }

  @Test
  void getPolicyByIdDelegatesToConfigurationServiceTest() {
    PolicyDto expected = samplePolicyDto();
    when(configurationService.getPolicyById(1)).thenReturn(expected);

    PolicyDto result = policyService.getPolicyById(1);

    assertSame(expected, result);
    verify(configurationService).getPolicyById(1);
    verifyNoMoreInteractions(configurationService, managementService);
  }

  @Test
  void getHolidaysForPolicyDelegatesToConfigurationServiceTest() {
    List<HolidayDto> expected = List.of(new HolidayDto(5, "Nowy Rok", LocalDate.of(2026, 1, 1)));
    when(configurationService.getHolidaysForPolicy(1)).thenReturn(expected);

    List<HolidayDto> result = policyService.getHolidaysForPolicy(1);

    assertSame(expected, result);
    verify(configurationService).getHolidaysForPolicy(1);
    verifyNoMoreInteractions(configurationService, managementService);
  }

  @Test
  void assignHolidayToPolicyDelegatesToConfigurationServiceTest() {
    AssignHolidayRequest request = new AssignHolidayRequest(5);

    policyService.assignHolidayToPolicy(1, request);

    verify(configurationService).assignHolidayToPolicy(1, request);
    verifyNoMoreInteractions(configurationService, managementService);
  }

  @Test
  void removeHolidayFromPolicyDelegatesToConfigurationServiceTest() {
    policyService.removeHolidayFromPolicy(1, 5);

    verify(configurationService).removeHolidayFromPolicy(1, 5);
    verifyNoMoreInteractions(configurationService, managementService);
  }

  @Test
  void deletePolicyDelegatesToConfigurationServiceTest() {
    policyService.deletePolicy(1);

    verify(configurationService).deletePolicy(1);
    verifyNoMoreInteractions(configurationService, managementService);
  }

  @Test
  void assignHolidaysToPolicyBulkDelegatesToConfigurationServiceTest() {
    MassAssignHolidayRequest request = new MassAssignHolidayRequest(List.of(5, 6));

    policyService.assignHolidaysToPolicyBulk(1, request);

    verify(configurationService).assignHolidaysToPolicyBulk(1, request);
    verifyNoMoreInteractions(configurationService, managementService);
  }

  @Test
  void assignPolicyToUserDelegatesToManagementServiceTest() {
    AssignPolicyRequest request = new AssignPolicyRequest(1, LocalDate.of(2026, 1, 1));

    policyService.assignPolicyToUser(7, request);

    verify(managementService).assignPolicyToUser(7, request);
    verifyNoMoreInteractions(configurationService, managementService);
  }

  @Test
  void assignPolicyToUsersBulkDelegatesToManagementServiceTest() {
    MassAssignPolicyRequest request = new MassAssignPolicyRequest(
        1, List.of(7, 8), LocalDate.of(2026, 1, 1));

    policyService.assignPolicyToUsersBulk(request);

    verify(managementService).assignPolicyToUsersBulk(request);
    verifyNoMoreInteractions(configurationService, managementService);
  }
}
