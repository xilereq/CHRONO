package pl.chrono.timemanager.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pl.chrono.dbmanager.model.Policy;
import pl.chrono.dbmanager.model.User;
import pl.chrono.dbmanager.model.UsersPolicy;
import pl.chrono.dbmanager.repository.PolicyRepository;
import pl.chrono.dbmanager.repository.UserRepository;
import pl.chrono.dbmanager.repository.UsersPolicyRepository;
import pl.chrono.timemanager.dto.request.AssignPolicyRequest;
import pl.chrono.timemanager.exception.PolicyException;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PolicyManagementServiceTest {

  @Mock
  private UsersPolicyRepository usersPolicyRepository;

  @Mock
  private PolicyRepository policyRepository;

  @Mock
  private UserRepository userRepository;

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

    verifyNoInteractions(policyRepository, usersPolicyRepository, userRepository);
  }

  @Test
  void noPolicyFoundExceptionTest() {
    Integer userId = 1;
    Integer invalidPolicyId = 999;
    AssignPolicyRequest request = new AssignPolicyRequest(
        invalidPolicyId, LocalDate.now().plusDays(1));

    when(policyRepository.findById(invalidPolicyId)).thenReturn(Optional.empty());

    assertThrows(
        PolicyException.NotFound.class,
        () -> policyManagementService.assignPolicyToUser(userId, request)
    );

    verifyNoInteractions(usersPolicyRepository, userRepository);
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

    when(policyRepository.findById(policyId)).thenReturn(Optional.of(policy));
    when(usersPolicyRepository.findActiveByUserId(userId)).thenReturn(Optional.of(oldAssignment));

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

    when(policyRepository.findById(newPolicyId)).thenReturn(Optional.of(newPolicy));
    when(usersPolicyRepository.findActiveByUserId(userId)).thenReturn(Optional.of(oldAssignment));

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

    when(policyRepository.findById(policyId)).thenReturn(Optional.of(newPolicy));
    when(usersPolicyRepository.findActiveByUserId(userId)).thenReturn(
        Optional.empty());
    when(userRepository.getReferenceById(userId)).thenReturn(user);

    policyManagementService.assignPolicyToUser(userId, request);

    ArgumentCaptor<UsersPolicy> captor = ArgumentCaptor.forClass(UsersPolicy.class);
    verify(usersPolicyRepository, times(1)).save(captor.capture());

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

    when(policyRepository.findById(newPolicyId)).thenReturn(Optional.of(newPolicy));
    when(usersPolicyRepository.findActiveByUserId(userId)).thenReturn(Optional.of(oldAssignment));
    when(userRepository.getReferenceById(userId)).thenReturn(user);

    policyManagementService.assignPolicyToUser(userId, request);

    ArgumentCaptor<UsersPolicy> captor = ArgumentCaptor.forClass(UsersPolicy.class);
    verify(usersPolicyRepository, times(2)).save(captor.capture());

    UsersPolicy updatedOld = captor.getAllValues().get(0);
    UsersPolicy createdNew = captor.getAllValues().get(1);

    assertEquals(startDate.minusDays(1), updatedOld.getValidTo());

    assertEquals(newPolicy, createdNew.getPolicy());
    assertEquals(startDate, createdNew.getValidFrom());
  }
}
