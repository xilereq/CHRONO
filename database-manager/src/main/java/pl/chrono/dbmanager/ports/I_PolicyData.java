package pl.chrono.dbmanager.ports;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import pl.chrono.dbmanager.model.Holiday;
import pl.chrono.dbmanager.model.HolidayPolicy;
import pl.chrono.dbmanager.model.Policy;
import pl.chrono.dbmanager.model.UsersPolicy;

public interface I_PolicyData {
  Policy savePolicy(Policy policy);

  Optional<Policy> findPolicyById(Integer policyId);

  List<Policy> findAllPolicies();

  void deletePolicy(Policy policy);

  HolidayPolicy saveHolidayPolicy(HolidayPolicy holidayPolicy);

  List<HolidayPolicy> saveAllHolidayPolicies(Collection<HolidayPolicy> holidayPolicies);

  void deleteHolidayPolicy(HolidayPolicy holidayPolicy);

  void deleteHolidayPoliciesByPolicy(Policy policy);

  boolean existsHolidayPolicy(Policy policy, Holiday holiday);

  List<HolidayPolicy> findHolidayPoliciesByPolicy(Policy policy);

  Optional<HolidayPolicy> findHolidayPolicyByPolicyAndHoliday(Policy policy, Holiday holiday);

  UsersPolicy saveUsersPolicy(UsersPolicy usersPolicy);

  Optional<UsersPolicy> findActiveUsersPolicyByUserId(Integer userId);
}
