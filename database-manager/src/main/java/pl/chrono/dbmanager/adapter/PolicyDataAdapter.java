package pl.chrono.dbmanager.adapter;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import pl.chrono.dbmanager.model.Holiday;
import pl.chrono.dbmanager.model.HolidayPolicy;
import pl.chrono.dbmanager.model.Policy;
import pl.chrono.dbmanager.model.UsersPolicy;
import pl.chrono.dbmanager.ports.I_PolicyData;
import pl.chrono.dbmanager.repository.HolidayPolicyRepository;
import pl.chrono.dbmanager.repository.PolicyRepository;
import pl.chrono.dbmanager.repository.UsersPolicyRepository;

@Component
@RequiredArgsConstructor
public class PolicyDataAdapter implements I_PolicyData {
  private final PolicyRepository policyRepository;
  private final HolidayPolicyRepository holidayPolicyRepository;
  private final UsersPolicyRepository usersPolicyRepository;

  @Override
  public Policy savePolicy(Policy policy) {
    return policyRepository.save(policy);
  }

  @Override
  public Optional<Policy> findPolicyById(Integer policyId) {
    return policyRepository.findById(policyId);
  }

  @Override
  public List<Policy> findAllPolicies() {
    return policyRepository.findAll();
  }

  @Override
  public void deletePolicy(Policy policy) {
    policyRepository.delete(policy);
  }

  @Override
  public HolidayPolicy saveHolidayPolicy(HolidayPolicy holidayPolicy) {
    return holidayPolicyRepository.save(holidayPolicy);
  }

  @Override
  public List<HolidayPolicy> saveAllHolidayPolicies(Collection<HolidayPolicy> holidayPolicies) {
    return holidayPolicyRepository.saveAll(holidayPolicies);
  }

  @Override
  public void deleteHolidayPolicy(HolidayPolicy holidayPolicy) {
    holidayPolicyRepository.delete(holidayPolicy);
  }

  @Override
  public void deleteHolidayPoliciesByPolicy(Policy policy) {
    holidayPolicyRepository.deleteByPolicy(policy);
  }

  @Override
  public boolean existsHolidayPolicy(Policy policy, Holiday holiday) {
    return holidayPolicyRepository.existsByPolicyAndHoliday(policy, holiday);
  }

  @Override
  public List<HolidayPolicy> findHolidayPoliciesByPolicy(Policy policy) {
    return holidayPolicyRepository.findByPolicy(policy);
  }

  @Override
  public Optional<HolidayPolicy> findHolidayPolicyByPolicyAndHoliday(
      Policy policy, Holiday holiday) {
    return holidayPolicyRepository.findByPolicyAndHoliday(policy, holiday);
  }

  @Override
  public UsersPolicy saveUsersPolicy(UsersPolicy usersPolicy) {
    return usersPolicyRepository.save(usersPolicy);
  }

  @Override
  public Optional<UsersPolicy> findActiveUsersPolicyByUserId(Integer userId) {
    return usersPolicyRepository.findActiveByUserId(userId);
  }
}
