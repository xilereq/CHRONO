package pl.chrono.timemanager.api;

import java.util.List;
import pl.chrono.timemanager.dto.request.AssignHolidayRequest;
import pl.chrono.timemanager.dto.request.AssignPolicyRequest;
import pl.chrono.timemanager.dto.request.CreatePolicyRequest;
import pl.chrono.timemanager.dto.request.MassAssignHolidayRequest;
import pl.chrono.timemanager.dto.request.MassAssignPolicyRequest;
import pl.chrono.timemanager.dto.response.HolidayDto;
import pl.chrono.timemanager.dto.response.PolicyDto;

public interface I_PolicyService {
  PolicyDto createPolicy(CreatePolicyRequest request);

  List<PolicyDto> getAllPolicies();

  PolicyDto getPolicyById(Integer policyId);

  List<HolidayDto> getHolidaysForPolicy(Integer policyId);

  void assignHolidayToPolicy(Integer policyId, AssignHolidayRequest request);

  void removeHolidayFromPolicy(Integer policyId, Integer holidayId);

  void deletePolicy(Integer policyId);

  void assignHolidaysToPolicyBulk(Integer policyId, MassAssignHolidayRequest request);

  void assignPolicyToUser(Integer userId, AssignPolicyRequest request);

  void assignPolicyToUsersBulk(MassAssignPolicyRequest request);
}
