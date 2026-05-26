package pl.chrono.timemanager.service;


import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.chrono.dbmanager.model.Policy;
import pl.chrono.dbmanager.model.User;
import pl.chrono.dbmanager.model.UsersPolicy;
import pl.chrono.dbmanager.repository.PolicyRepository;
import pl.chrono.dbmanager.repository.UserRepository;
import pl.chrono.dbmanager.repository.UsersPolicyRepository;
import pl.chrono.logmanager.audit.LogActivity;
import pl.chrono.timemanager.dto.request.AssignPolicyRequest;
import pl.chrono.timemanager.exception.ErrorCodes;
import pl.chrono.timemanager.exception.PolicyException;

@Service
@RequiredArgsConstructor
public class PolicyManagementService {
  private final UsersPolicyRepository usersPolicyRepository;
  private final PolicyRepository policyRepository;
  private final UserRepository userRepository;

  @Transactional
  @LogActivity(action = "ASSIGNED_POLICY", details = "Assigned policy to user")
  public void assignPolicyToUser(Integer userId, AssignPolicyRequest request) {
    if (request.startDate().isBefore(LocalDate.now())) {
      throw new PolicyException.InvalidDate(
          ErrorCodes.PAST_START_DATE,"Start date cannot be in the past");
    }

    Policy newPolicy = policyRepository.findById(request.policyId())
        .orElseThrow(() -> new PolicyException.NotFound(request.policyId()));

    usersPolicyRepository.findActiveByUserId(userId).ifPresent(oldPolicy -> {
      if (oldPolicy.getPolicy().getPolicyId().equals(request.policyId())) {
        throw new PolicyException.AlreadyAssigned();
      }
      if (request.startDate().isBefore(oldPolicy.getValidFrom())) {
        throw new PolicyException.InvalidDate(ErrorCodes.START_DATE_CONFLICT,
            "New policy start date cannot be before current policy start date.");
      }

      oldPolicy.setValidTo(request.startDate().minusDays(1));
      usersPolicyRepository.save(oldPolicy);
    });

    UsersPolicy newAssignment = new UsersPolicy();
    User user = userRepository.getReferenceById(userId);
    newAssignment.setUser(user);
    newAssignment.setPolicy(newPolicy);
    newAssignment.setValidFrom(request.startDate());
    newAssignment.setValidTo(null);
    usersPolicyRepository.save(newAssignment);
  }
}
