package pl.chrono.timemanager.service;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.chrono.dbmanager.model.Holiday;
import pl.chrono.dbmanager.model.HolidayPolicy;
import pl.chrono.dbmanager.model.Policy;
import pl.chrono.dbmanager.ports.I_HolidayData;
import pl.chrono.dbmanager.ports.I_PolicyData;
import pl.chrono.logmanager.audit.LogActivity;
import pl.chrono.timemanager.dto.request.AssignHolidayRequest;
import pl.chrono.timemanager.dto.request.CreatePolicyRequest;
import pl.chrono.timemanager.dto.request.MassAssignHolidayRequest;
import pl.chrono.timemanager.dto.response.HolidayDto;
import pl.chrono.timemanager.dto.response.PolicyDto;
import pl.chrono.timemanager.exception.HolidayException;
import pl.chrono.timemanager.exception.PolicyException;
import pl.chrono.timemanager.mapper.HolidayMapper;
import pl.chrono.timemanager.mapper.PolicyMapper;

@Service
@RequiredArgsConstructor
public class PolicyConfigurationService {
  private final I_PolicyData policyData;
  private final PolicyMapper policyMapper;
  private final I_HolidayData holidayData;
  private final HolidayMapper holidayMapper;

  @Transactional
  public PolicyDto createPolicy(CreatePolicyRequest request) {
    Policy policy = policyMapper.toEntity(request);

    Policy saved = policyData.savePolicy(policy);

    return policyMapper.toDto(saved);

  }

  @Transactional(readOnly = true)
  public List<PolicyDto> getAllPolicies() {
    return policyData.findAllPolicies().stream()
        .map(policyMapper::toDto)
        .toList();
  }

  @Transactional(readOnly = true)
  public PolicyDto getPolicyById(Integer policyId) {
    return policyData.findPolicyById(policyId)
        .map(policyMapper::toDto)
        .orElseThrow(() -> new PolicyException.NotFound(policyId));
  }

  @Transactional(readOnly = true)
  public List<HolidayDto> getHolidaysForPolicy(Integer policyId) {
    Policy policy = policyData.findPolicyById(policyId)
        .orElseThrow(() -> new PolicyException.NotFound(policyId));

    return policyData.findHolidayPoliciesByPolicy(policy).stream()
        .map(HolidayPolicy::getHoliday)
        .map(holidayMapper::toDto)
        .toList();
  }

  @Transactional
  @LogActivity(action = "ASSIGN_HOLIDAY", details = "Assigned holiday to policy")
  public void assignHolidayToPolicy(Integer policyId, AssignHolidayRequest request) {
    Policy policy = policyData.findPolicyById(policyId)
        .orElseThrow(() -> new PolicyException.NotFound(policyId));

    Holiday holiday = holidayData.findHolidayById(request.holidayId())
        .orElseThrow(() -> new HolidayException.NotFound(request.holidayId()));

    boolean alreadyAssigned = policyData.existsHolidayPolicy(policy, holiday);
    if (alreadyAssigned) {
      throw new HolidayException.AlreadyAssigned();
    }

    HolidayPolicy holidayPolicy = new HolidayPolicy();
    holidayPolicy.setPolicy(policy);
    holidayPolicy.setHoliday(holiday);

    policyData.saveHolidayPolicy(holidayPolicy);
  }

  @Transactional
  @LogActivity(action = "REMOVED_POLICY_HOLIDAY", details = "Removed holiday from policy")
  public void removeHolidayFromPolicy(Integer policyId, Integer holidayId) {
    Policy policy = policyData.findPolicyById(policyId)
        .orElseThrow(() -> new PolicyException.NotFound(policyId));

    Holiday holiday = holidayData.findHolidayById(holidayId)
        .orElseThrow(() -> new HolidayException.NotFound(holidayId));

    HolidayPolicy assignment = policyData.findHolidayPolicyByPolicyAndHoliday(policy, holiday)
        .orElseThrow(HolidayException.NotAssigned::new);

    policyData.deleteHolidayPolicy(assignment);
  }

  @Transactional
  @LogActivity(action = "DELETED_POLICY", details = "Deleted policy")
  public void deletePolicy(Integer policyId) {
    Policy policy = policyData.findPolicyById(policyId)
        .orElseThrow(() -> new PolicyException.NotFound(policyId));

    policyData.deleteHolidayPoliciesByPolicy(policy);

    policyData.deletePolicy(policy);
  }

  @Transactional
  @LogActivity(action = "BULK_ASSIGN_HOLIDAYS", details = "Assigned multiple holidays to policy")
  public void assignHolidaysToPolicyBulk(Integer policyId, MassAssignHolidayRequest request) {
    Policy policy = policyData.findPolicyById(policyId)
        .orElseThrow(() -> new PolicyException.NotFound(policyId));

    List<Holiday> holidaysToAssign = holidayData.findHolidaysByIds(request.holidayIds());
    if (holidaysToAssign.isEmpty()) {
      throw new HolidayException.NoneFound();
    }

    List<HolidayPolicy> existingAssignments = policyData.findHolidayPoliciesByPolicy(policy);
    Set<Integer> existingHolidayIds = existingAssignments.stream()
        .map(hp -> hp.getHoliday().getHolidayId())
        .collect(Collectors.toSet());

    List<HolidayPolicy> newAssignments = holidaysToAssign.stream()
        .filter(holiday -> !existingHolidayIds.contains(holiday.getHolidayId()))
        .map(holiday -> {
          HolidayPolicy hp = new HolidayPolicy();
          hp.setPolicy(policy);
          hp.setHoliday(holiday);
          return hp;
        })
        .toList();

    if (!newAssignments.isEmpty()) {
      policyData.saveAllHolidayPolicies(newAssignments);
    }
  }
}
