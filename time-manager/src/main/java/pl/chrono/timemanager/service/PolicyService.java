package pl.chrono.timemanager.service;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import pl.chrono.timemanager.api.I_PolicyService;
import pl.chrono.timemanager.dto.request.AssignHolidayRequest;
import pl.chrono.timemanager.dto.request.AssignPolicyRequest;
import pl.chrono.timemanager.dto.request.CreatePolicyRequest;
import pl.chrono.timemanager.dto.request.MassAssignHolidayRequest;
import pl.chrono.timemanager.dto.request.MassAssignPolicyRequest;
import pl.chrono.timemanager.dto.response.HolidayDto;
import pl.chrono.timemanager.dto.response.PolicyDto;

@Service
@RequiredArgsConstructor
public class PolicyService implements I_PolicyService {

  private final PolicyConfigurationService configurationService;
  private final PolicyManagementService managementService;

  @Override
  public PolicyDto createPolicy(CreatePolicyRequest request) {
    return configurationService.createPolicy(request);
  }

  @Override
  public List<PolicyDto> getAllPolicies() {
    return configurationService.getAllPolicies();
  }

  @Override
  public PolicyDto getPolicyById(Integer policyId) {
    return configurationService.getPolicyById(policyId);
  }

  @Override
  public List<HolidayDto> getHolidaysForPolicy(Integer policyId) {
    return configurationService.getHolidaysForPolicy(policyId);
  }

  @Override
  public void assignHolidayToPolicy(Integer policyId, AssignHolidayRequest request) {
    configurationService.assignHolidayToPolicy(policyId, request);
  }

  @Override
  public void removeHolidayFromPolicy(Integer policyId, Integer holidayId) {
    configurationService.removeHolidayFromPolicy(policyId, holidayId);
  }

  @Override
  public void deletePolicy(Integer policyId) {
    configurationService.deletePolicy(policyId);
  }

  @Override
  public void assignHolidaysToPolicyBulk(Integer policyId, MassAssignHolidayRequest request) {
    configurationService.assignHolidaysToPolicyBulk(policyId, request);
  }

  @Override
  public void assignPolicyToUser(Integer userId, AssignPolicyRequest request) {
    managementService.assignPolicyToUser(userId, request);
  }

  @Override
  public void assignPolicyToUsersBulk(MassAssignPolicyRequest request) {
    managementService.assignPolicyToUsersBulk(request);
  }
}
