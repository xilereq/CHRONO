package pl.chrono.timemanager.service;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.chrono.dbmanager.model.Holiday;
import pl.chrono.dbmanager.model.HolidayPolicy;
import pl.chrono.dbmanager.model.Policy;
import pl.chrono.dbmanager.repository.HolidayPolicyRepository;
import pl.chrono.dbmanager.repository.HolidayRepository;
import pl.chrono.dbmanager.repository.PolicyRepository;
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
  private final PolicyRepository policyRepository;
  private final PolicyMapper policyMapper;
  private final HolidayRepository holidayRepository;
  private final HolidayPolicyRepository holidayPolicyRepository;
  private final HolidayMapper holidayMapper;

  @Transactional
  public PolicyDto createPolicy(CreatePolicyRequest request) {
    Policy policy = policyMapper.toEntity(request);

    Policy saved = policyRepository.save(policy);

    return policyMapper.toDto(saved);

  }

  @Transactional(readOnly = true)
  public List<PolicyDto> getAllPolicies() {
    return policyRepository.findAll().stream()
        .map(policyMapper::toDto)
        .toList();
  }

  @Transactional(readOnly = true)
  public PolicyDto getPolicyById(Integer policyId) {
    return policyRepository.findById(policyId)
        .map(policyMapper::toDto)
        .orElseThrow(() -> new PolicyException.NotFound(policyId));
  }

  @Transactional(readOnly = true)
  public List<HolidayDto> getHolidaysForPolicy(Integer policyId) {
    Policy policy = policyRepository.findById(policyId)
        .orElseThrow(() -> new PolicyException.NotFound(policyId));

    return holidayPolicyRepository.findByPolicy(policy).stream()
        .map(HolidayPolicy::getHoliday)
        .map(holidayMapper::toDto)
        .toList();
  }

  @Transactional
  @LogActivity(action = "ASSIGN_HOLIDAY", details = "Assigned holiday to policy")
  public void assignHolidayToPolicy(Integer policyId, AssignHolidayRequest request) {
    Policy policy = policyRepository.findById(policyId)
        .orElseThrow(() -> new PolicyException.NotFound(policyId));

    Holiday holiday = holidayRepository.findById(request.holidayId())
        .orElseThrow(() -> new HolidayException.NotFound(request.holidayId()));

    boolean alreadyAssigned = holidayPolicyRepository.existsByPolicyAndHoliday(policy, holiday);
    if (alreadyAssigned) {
      throw new HolidayException.AlreadyAssigned();
    }

    HolidayPolicy holidayPolicy = new HolidayPolicy();
    holidayPolicy.setPolicy(policy);
    holidayPolicy.setHoliday(holiday);

    holidayPolicyRepository.save(holidayPolicy);
  }

  @Transactional
  @LogActivity(action = "REMOVED_POLICY_HOLIDAY", details = "Removed holiday from policy")
  public void removeHolidayFromPolicy(Integer policyId, Integer holidayId) {
    Policy policy = policyRepository.findById(policyId)
        .orElseThrow(() -> new PolicyException.NotFound(policyId));

    Holiday holiday = holidayRepository.findById(holidayId)
        .orElseThrow(() -> new HolidayException.NotFound(holidayId));

    HolidayPolicy assignment = holidayPolicyRepository.findByPolicyAndHoliday(policy, holiday)
        .orElseThrow(HolidayException.NotAssigned::new);

    holidayPolicyRepository.delete(assignment);
  }

  @Transactional
  @LogActivity(action = "DELETED_POLICY", details = "Deleted policy")
  public void deletePolicy(Integer policyId) {
    Policy policy = policyRepository.findById(policyId)
        .orElseThrow(() -> new PolicyException.NotFound(policyId));

    holidayPolicyRepository.deleteByPolicy(policy);

    policyRepository.delete(policy);
  }

  @Transactional
  @LogActivity(action = "BULK_ASSIGN_HOLIDAYS", details = "Assigned multiple holidays to policy")
  public void assignHolidaysToPolicyBulk(Integer policyId, MassAssignHolidayRequest request) {
    Policy policy = policyRepository.findById(policyId)
        .orElseThrow(() -> new PolicyException.NotFound(policyId));

    List<Holiday> holidaysToAssign = holidayRepository.findAllById(request.holidayIds());
    if (holidaysToAssign.isEmpty()) {
      throw new IllegalArgumentException("No valid holidays found for provided IDs.");
    }

    List<HolidayPolicy> existingAssignments = holidayPolicyRepository.findByPolicy(policy);
    java.util.Set<Integer> existingHolidayIds = existingAssignments.stream()
        .map(hp -> hp.getHoliday().getHolidayId())
        .collect(java.util.stream.Collectors.toSet());

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
      holidayPolicyRepository.saveAll(newAssignments);
    }
  }
}
