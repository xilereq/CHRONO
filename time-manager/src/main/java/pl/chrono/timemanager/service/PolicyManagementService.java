package pl.chrono.timemanager.service;


import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.chrono.dbmanager.model.Policy;
import pl.chrono.dbmanager.model.User;
import pl.chrono.dbmanager.model.UsersPolicy;
import pl.chrono.dbmanager.ports.I_PolicyData;
import pl.chrono.dbmanager.ports.I_UserData;
import pl.chrono.logmanager.audit.LogActivity;
import pl.chrono.timemanager.dto.request.AssignPolicyRequest;
import pl.chrono.timemanager.dto.request.MassAssignPolicyRequest;
import pl.chrono.timemanager.exception.ErrorCodes;
import pl.chrono.timemanager.exception.PolicyException;

@Service
@RequiredArgsConstructor
public class PolicyManagementService {
  private final I_PolicyData policyData;
  private final I_UserData userData;

  @Transactional
  @LogActivity(action = "ASSIGNED_POLICY", details = "Assigned policy to user")
  public void assignPolicyToUser(Integer userId, AssignPolicyRequest request) {
    if (request.startDate().isBefore(LocalDate.now())) {
      throw new PolicyException.InvalidDate(
          ErrorCodes.PAST_START_DATE, "Start date cannot be in the past");
    }

    Policy newPolicy = policyData.findPolicyById(request.policyId())
        .orElseThrow(() -> new PolicyException.NotFound(request.policyId()));

    policyData.findActiveUsersPolicyByUserId(userId).ifPresent(oldPolicy -> {
      if (oldPolicy.getPolicy().getPolicyId().equals(request.policyId())) {
        throw new PolicyException.AlreadyAssigned();
      }
      if (request.startDate().isBefore(oldPolicy.getValidFrom())) {
        throw new PolicyException.InvalidDate(
            ErrorCodes.START_DATE_CONFLICT,
            "New policy start date cannot be before current policy start date."
        );
      }

      oldPolicy.setValidTo(request.startDate().minusDays(1));
      policyData.saveUsersPolicy(oldPolicy);
    });

    UsersPolicy newAssignment = new UsersPolicy();
    User user = userData.getUserReference(userId);
    newAssignment.setUser(user);
    newAssignment.setPolicy(newPolicy);
    newAssignment.setValidFrom(request.startDate());
    newAssignment.setValidTo(null);
    policyData.saveUsersPolicy(newAssignment);
  }

  @Transactional
  @LogActivity(action = "MASS_ASSIGNED_POLICY", details = "Assigned policy to multiple users")
  public void assignPolicyToUsersBulk(MassAssignPolicyRequest request) {
    AssignPolicyRequest singleRequest = new AssignPolicyRequest(
        request.policyId(), request.startDate());

    for (Integer userId : request.userIds()) {
      assignPolicyToUser(userId, singleRequest);
    }
  }
}
