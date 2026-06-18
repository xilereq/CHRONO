package pl.chrono.timemanager.service;

import jakarta.persistence.EntityManager;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import pl.chrono.dbmanager.model.Policy;
import pl.chrono.dbmanager.model.Role;
import pl.chrono.dbmanager.model.User;
import pl.chrono.dbmanager.model.UsersPolicy;
import pl.chrono.dbmanager.repository.PolicyRepository;
import pl.chrono.dbmanager.repository.RoleRepository;
import pl.chrono.dbmanager.repository.UserRepository;
import pl.chrono.dbmanager.repository.UsersPolicyRepository;
import pl.chrono.timemanager.TestConfig;
import pl.chrono.timemanager.dto.request.AssignPolicyRequest;
import pl.chrono.timemanager.dto.request.MassAssignPolicyRequest;
import pl.chrono.timemanager.exception.PolicyException;

@SpringBootTest(classes = TestConfig.class)
@Transactional
class PolicyManagementServiceIT {

  @Autowired
  private PolicyManagementService policyManagementService;

  @Autowired
  private PolicyRepository policyRepository;

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private UsersPolicyRepository usersPolicyRepository;

  @Autowired
  private RoleRepository roleRepository;

  @Autowired
  private EntityManager entityManager;

  private Policy policyA;
  private Policy policyB;
  private User user1;
  private User user2;

  @BeforeEach
  void setUp() {
    policyA = persistPolicy("Standard", 8);
    policyB = persistPolicy("Elastyczna", 7);
    user1 = persistUser("jan.kowalski@chrono.pl");
    user2 = persistUser("anna.nowak@chrono.pl");
  }

  @Test
  void assignPolicyToUser_createsNewAssignment() {
    LocalDate startDate = LocalDate.now().plusDays(1);
    AssignPolicyRequest request = new AssignPolicyRequest(policyA.getPolicyId(), startDate);

    policyManagementService.assignPolicyToUser(user1.getUserId(), request);

    entityManager.flush();
    entityManager.clear();

    UsersPolicy active = usersPolicyRepository
        .findActiveByUserId(user1.getUserId()).orElseThrow();
    assertThat(active.getPolicy().getPolicyId()).isEqualTo(policyA.getPolicyId());
    assertThat(active.getValidFrom()).isEqualTo(startDate);
    assertThat(active.getValidTo()).isNull();
  }

  @Test
  void assignPolicyToUser_closesOldPolicy_whenAssigningNew() {
    LocalDate firstStart = LocalDate.now().plusDays(1);
    LocalDate secondStart = LocalDate.now().plusDays(10);

    policyManagementService.assignPolicyToUser(user1.getUserId(),
        new AssignPolicyRequest(policyA.getPolicyId(), firstStart));

    entityManager.flush();
    entityManager.clear();

    policyManagementService.assignPolicyToUser(user1.getUserId(),
        new AssignPolicyRequest(policyB.getPolicyId(), secondStart));

    entityManager.flush();
    entityManager.clear();

    UsersPolicy active = usersPolicyRepository
        .findActiveByUserId(user1.getUserId()).orElseThrow();
    assertThat(active.getPolicy().getPolicyId()).isEqualTo(policyB.getPolicyId());
    assertThat(active.getValidTo()).isNull();

    List<UsersPolicy> all = usersPolicyRepository.findAll().stream()
        .filter(up -> up.getUser().getUserId().equals(user1.getUserId()))
        .toList();
    assertThat(all).hasSize(2);
    assertThat(all.stream().filter(up -> up.getValidTo() != null).count()).isEqualTo(1);
  }

  @Test
  void assignPolicyToUser_throwsAlreadyAssigned_whenSamePolicyAssigned() {
    LocalDate startDate = LocalDate.now().plusDays(1);
    policyManagementService.assignPolicyToUser(user1.getUserId(),
        new AssignPolicyRequest(policyA.getPolicyId(), startDate));
    entityManager.flush();
    entityManager.clear();

    assertThatThrownBy(() ->
        policyManagementService.assignPolicyToUser(user1.getUserId(),
            new AssignPolicyRequest(policyA.getPolicyId(), startDate.plusDays(5))))
        .isInstanceOf(PolicyException.AlreadyAssigned.class);
  }

  @Test
  void assignPolicyToUser_throwsInvalidDate_whenStartDateInPast() {
    LocalDate pastDate = LocalDate.now().minusDays(1);
    AssignPolicyRequest request = new AssignPolicyRequest(policyA.getPolicyId(), pastDate);

    assertThatThrownBy(() ->
        policyManagementService.assignPolicyToUser(user1.getUserId(), request))
        .isInstanceOf(PolicyException.InvalidDate.class);
  }

  @Test
  void assignPolicyToUser_throwsInvalidDate_whenNewStartBeforeCurrentStart() {
    LocalDate firstStart = LocalDate.now().plusDays(10);
    policyManagementService.assignPolicyToUser(user1.getUserId(),
        new AssignPolicyRequest(policyA.getPolicyId(), firstStart));
    entityManager.flush();
    entityManager.clear();

    assertThatThrownBy(() ->
        policyManagementService.assignPolicyToUser(user1.getUserId(),
            new AssignPolicyRequest(policyB.getPolicyId(), firstStart.minusDays(5))))
        .isInstanceOf(PolicyException.InvalidDate.class);
  }

  @Test
  void assignPolicyToUser_throwsNotFound_whenPolicyDoesNotExist() {
    AssignPolicyRequest request = new AssignPolicyRequest(999999, LocalDate.now().plusDays(1));

    assertThatThrownBy(() ->
        policyManagementService.assignPolicyToUser(user1.getUserId(), request))
        .isInstanceOf(PolicyException.NotFound.class);
  }


  @Test
  void assignPolicyToUsersBulk_assignsPolicyToAllUsers() {
    LocalDate startDate = LocalDate.now().plusDays(1);
    MassAssignPolicyRequest request = new MassAssignPolicyRequest(
        policyA.getPolicyId(),
        List.of(user1.getUserId(), user2.getUserId()),
        startDate
    );

    policyManagementService.assignPolicyToUsersBulk(request);

    entityManager.flush();
    entityManager.clear();

    assertThat(usersPolicyRepository.findActiveByUserId(user1.getUserId())).isPresent();
    assertThat(usersPolicyRepository.findActiveByUserId(user2.getUserId())).isPresent();
  }

  @Test
  void assignPolicyToUsersBulk_throwsNotFound_whenPolicyDoesNotExist() {
    MassAssignPolicyRequest request = new MassAssignPolicyRequest(
        999999,
        List.of(user1.getUserId()),
        LocalDate.now().plusDays(1)
    );

    assertThatThrownBy(() ->
        policyManagementService.assignPolicyToUsersBulk(request))
        .isInstanceOf(PolicyException.NotFound.class);
  }

  @Test
  void assignPolicyToUsersBulk_throwsInvalidDate_whenStartDateInPast() {
    MassAssignPolicyRequest request = new MassAssignPolicyRequest(
        policyA.getPolicyId(),
        List.of(user1.getUserId()),
        LocalDate.now().minusDays(1)
    );

    assertThatThrownBy(() ->
        policyManagementService.assignPolicyToUsersBulk(request))
        .isInstanceOf(PolicyException.InvalidDate.class);
  }

  private Policy persistPolicy(String name, int dailyHours) {
    Policy policy = new Policy();
    policy.setName(name);
    policy.setDailyHours(dailyHours);
    policy.setWorkingDays(Set.of(DayOfWeek.MONDAY, DayOfWeek.TUESDAY,
        DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY));
    return policyRepository.save(policy);
  }

  private User persistUser(String email) {
    Role role = roleRepository.findByName("ROLE_EMPLOYEE")
        .orElseGet(() -> {
          Role r = new Role();
          r.setName("ROLE_EMPLOYEE");
          return roleRepository.save(r);
        });

    User user = new User();
    user.setName("Jan");
    user.setSurname("Kowalski");
    user.setEmail(email);
    user.setPasswordHash("hashed");
    user.setRoles(Set.of(role));
    return userRepository.save(user);
  }
}
