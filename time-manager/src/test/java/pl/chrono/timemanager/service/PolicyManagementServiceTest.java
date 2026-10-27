package pl.chrono.timemanager.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import pl.chrono.dbmanager.model.Policy;
import pl.chrono.dbmanager.model.User;
import pl.chrono.dbmanager.model.UsersPolicy;
import pl.chrono.dbmanager.ports.I_PolicyData;
import pl.chrono.dbmanager.ports.I_UserData;
import pl.chrono.timemanager.dto.request.AssignPolicyRequest;
import pl.chrono.timemanager.dto.request.MassAssignPolicyRequest;
import pl.chrono.timemanager.exception.PolicyException;

@ExtendWith(MockitoExtension.class)
class PolicyManagementServiceTest {

  @Mock
  private I_UserData userData;

  @Mock
  private I_PolicyData policyData;

  @InjectMocks
  private PolicyManagementService policyManagementService;

  @Test
  void startDateInPastExceptionTest() {
    Integer userId = 1;
    AssignPolicyRequest request = new AssignPolicyRequest(1, LocalDate.now().minusDays(1));

    assertThrows(
        PolicyException.InvalidDate.class,
        () -> policyManagementService.assignPolicyToUser(userId, request)
    );

    verifyNoInteractions(userData, policyData);
  }

  @Test
  void noPolicyFoundExceptionTest() {
    Integer userId = 1;
    Integer invalidPolicyId = 999;
    AssignPolicyRequest request = new AssignPolicyRequest(
        invalidPolicyId, LocalDate.now().plusDays(1));

    when(policyData.findPolicyById(invalidPolicyId)).thenReturn(Optional.empty());

    assertThrows(
        PolicyException.NotFound.class,
        () -> policyManagementService.assignPolicyToUser(userId, request)
    );

    verify(policyData).findPolicyById(invalidPolicyId);
    verifyNoMoreInteractions(policyData);
    verifyNoInteractions(userData);
  }

  @Test
  void policyAlreadyAssignedExceptionTest() {
    Integer userId = 1;
    Integer policyId = 2;
    AssignPolicyRequest request = new AssignPolicyRequest(policyId, LocalDate.now().plusDays(5));

    Policy policy = new Policy();
    policy.setPolicyId(policyId);

    UsersPolicy oldAssignment = new UsersPolicy();
    oldAssignment.setPolicy(policy);

    when(policyData.findPolicyById(policyId)).thenReturn(Optional.of(policy));
    when(policyData.findActiveUsersPolicyByUserId(userId)).thenReturn(Optional.of(oldAssignment));

    assertThrows(
        PolicyException.AlreadyAssigned.class,
        () -> policyManagementService.assignPolicyToUser(userId, request)
    );
  }

  @Test
  void startDateConflictExceptionTest() {
    Integer userId = 1;
    Integer newPolicyId = 2;
    Integer oldPolicyId = 1;

    AssignPolicyRequest request = new AssignPolicyRequest(newPolicyId, LocalDate.now().plusDays(1));

    Policy newPolicy = new Policy();
    newPolicy.setPolicyId(newPolicyId);

    Policy oldPolicy = new Policy();
    oldPolicy.setPolicyId(oldPolicyId);

    UsersPolicy oldAssignment = new UsersPolicy();
    oldAssignment.setPolicy(oldPolicy);
    oldAssignment.setValidFrom(LocalDate.now().plusDays(2));

    when(policyData.findPolicyById(newPolicyId)).thenReturn(Optional.of(newPolicy));
    when(policyData.findActiveUsersPolicyByUserId(userId)).thenReturn(Optional.of(oldAssignment));

    assertThrows(
        PolicyException.InvalidDate.class,
        () -> policyManagementService.assignPolicyToUser(userId, request)
    );
  }

  @Test
  void assignNewPolicyTest() {
    Integer userId = 1;
    Integer policyId = 2;
    LocalDate startDate = LocalDate.now().plusDays(1);
    AssignPolicyRequest request = new AssignPolicyRequest(policyId, startDate);

    Policy newPolicy = new Policy();
    newPolicy.setPolicyId(policyId);
    User user = new User();

    when(policyData.findPolicyById(policyId)).thenReturn(Optional.of(newPolicy));
    when(policyData.findActiveUsersPolicyByUserId(userId)).thenReturn(Optional.empty());
    when(userData.getUserReference(userId)).thenReturn(user);

    policyManagementService.assignPolicyToUser(userId, request);

    ArgumentCaptor<UsersPolicy> captor = ArgumentCaptor.forClass(UsersPolicy.class);
    verify(policyData, times(1)).saveUsersPolicy(captor.capture());

    UsersPolicy savedAssignment = captor.getValue();
    assertEquals(user, savedAssignment.getUser());
    assertEquals(newPolicy, savedAssignment.getPolicy());
    assertEquals(startDate, savedAssignment.getValidFrom());
    assertNull(savedAssignment.getValidTo());
  }

  @Test
  void changePolicyTest() {
    Integer userId = 1;
    Integer newPolicyId = 2;
    Integer oldPolicyId = 1;
    LocalDate startDate = LocalDate.now().plusDays(10);
    AssignPolicyRequest request = new AssignPolicyRequest(newPolicyId, startDate);

    Policy newPolicy = new Policy();
    newPolicy.setPolicyId(newPolicyId);
    User user = new User();

    Policy oldPolicy = new Policy();
    oldPolicy.setPolicyId(oldPolicyId);

    UsersPolicy oldAssignment = new UsersPolicy();
    oldAssignment.setPolicy(oldPolicy);
    oldAssignment.setValidFrom(LocalDate.now().plusDays(1));

    when(policyData.findPolicyById(newPolicyId)).thenReturn(Optional.of(newPolicy));
    when(policyData.findActiveUsersPolicyByUserId(userId)).thenReturn(Optional.of(oldAssignment));
    when(userData.getUserReference(userId)).thenReturn(user);

    policyManagementService.assignPolicyToUser(userId, request);

    ArgumentCaptor<UsersPolicy> captor = ArgumentCaptor.forClass(UsersPolicy.class);
    verify(policyData, times(2)).saveUsersPolicy(captor.capture());

    UsersPolicy updatedOld = captor.getAllValues().get(0);
    UsersPolicy createdNew = captor.getAllValues().get(1);

    assertEquals(startDate.minusDays(1), updatedOld.getValidTo());

    assertEquals(newPolicy, createdNew.getPolicy());
    assertEquals(startDate, createdNew.getValidFrom());
  }

  @Test
  void assignPolicyToUsersSuccessTest() {
    Integer policyId = 2;
    LocalDate startDate = LocalDate.now().plusDays(1);
    MassAssignPolicyRequest request = new MassAssignPolicyRequest(
        policyId, List.of(1, 2), startDate);

    Policy newPolicy = new Policy();
    newPolicy.setPolicyId(policyId);
    User user1 = new User();
    User user2 = new User();

    when(policyData.findPolicyById(policyId)).thenReturn(Optional.of(newPolicy));

    when(policyData.findActiveUsersPolicyByUserId(1)).thenReturn(Optional.empty());
    when(policyData.findActiveUsersPolicyByUserId(2)).thenReturn(Optional.empty());

    when(userData.getUserReference(1)).thenReturn(user1);
    when(userData.getUserReference(2)).thenReturn(user2);

    policyManagementService.assignPolicyToUsersBulk(request);

    verify(policyData, times(2)).saveUsersPolicy(any(UsersPolicy.class));
  }

  @Test
  void assignPolicyToUsersBulkEmptyListTest() {
    Integer policyId = 2;
    LocalDate startDate = LocalDate.now().plusDays(1);
    MassAssignPolicyRequest request = new MassAssignPolicyRequest(policyId, List.of(), startDate);

    policyManagementService.assignPolicyToUsersBulk(request);

    verifyNoInteractions(policyData, userData);
  }
}
