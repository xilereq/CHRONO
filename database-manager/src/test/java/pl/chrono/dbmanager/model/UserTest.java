package pl.chrono.dbmanager.model;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class UserTest {

  private User user;

  @BeforeEach
  void setUp() {
    user = new User();
  }

  @Test
  void roleTest() {
    Role role1 = new Role();
    role1.setName("ROLE_USER");
    Role role2 = new Role();
    role2.setName("ROLE_MANAGER");

    user.setRoles(Set.of(role1, role2));

    var authorities = user.getAuthorities();

    assertThat(authorities).hasSize(2);
    assertThat(authorities).extracting("authority")
        .containsExactlyInAnyOrder("ROLE_USER", "ROLE_MANAGER");
  }

  @Test
  void UserDetailsTest() {
    user.setEmail("test@example.com");
    user.setPasswordHash("secretHash123");
    user.setActive(true);

    assertThat(user.getUsername()).isEqualTo("test@example.com");
    assertThat(user.getPassword()).isEqualTo("secretHash123");
    assertThat(user.isEnabled()).isTrue();
  }

  @Test
  void noPoliciesTest() {
    assertThat(user.getActivePolicy()).isNull();
  }

  @Test
  void nullReturnForEmptyPolicyListTest() {
    user.setUserPolicies(java.util.Collections.emptyList());

    assertThat(user.getActivePolicy()).isNull();
  }

  @Test
  void activePolicyWithDeadlineInFutureTest() {
    Policy activePolicyInfo = new Policy();
    UsersPolicy usersPolicy = new UsersPolicy();
    usersPolicy.setPolicy(activePolicyInfo);

    usersPolicy.setValidFrom(LocalDate.now().minusDays(10));
    usersPolicy.setValidTo(LocalDate.now().plusDays(10));

    user.setUserPolicies(List.of(usersPolicy));

    Policy result = user.getActivePolicy();

    assertThat(result).isNotNull();
    assertThat(result).isEqualTo(activePolicyInfo);
  }

  @Test
  void activeCurrentPolicyTest() {
    Policy activePolicyInfo = new Policy();
    UsersPolicy usersPolicy = new UsersPolicy();
    usersPolicy.setPolicy(activePolicyInfo);
    usersPolicy.setValidFrom(LocalDate.now().minusDays(10));
    usersPolicy.setValidTo(null);

    user.setUserPolicies(List.of(usersPolicy));

    Policy result = user.getActivePolicy();

    assertThat(result).isNotNull();
  }

  @Test
  void expiredPolicyTest() {
    Policy activePolicyInfo = new Policy();
    UsersPolicy usersPolicy = new UsersPolicy();
    usersPolicy.setPolicy(activePolicyInfo);
    usersPolicy.setValidFrom(LocalDate.now().minusDays(20));
    usersPolicy.setValidTo(LocalDate.now().minusDays(5));

    user.setUserPolicies(List.of(usersPolicy));

    Policy result = user.getActivePolicy();

    assertThat(result).isNull();
  }

  @Test
  void nullValidFromTest() {
    Policy activePolicyInfo = new Policy();
    UsersPolicy usersPolicy = new UsersPolicy();
    usersPolicy.setPolicy(activePolicyInfo);
    usersPolicy.setValidFrom(null);
    usersPolicy.setValidTo(LocalDate.now().plusDays(10));

    user.setUserPolicies(List.of(usersPolicy));

    Policy result = user.getActivePolicy();

    assertThat(result).isNull();
  }

  @Test
  void futurePolicyTest() {
    Policy futurePolicy = new Policy();
    UsersPolicy usersPolicy = new UsersPolicy();
    usersPolicy.setPolicy(futurePolicy);
    usersPolicy.setValidFrom(LocalDate.now().plusDays(5));
    usersPolicy.setValidTo(LocalDate.now().plusDays(10));

    user.setUserPolicies(List.of(usersPolicy));

    Policy result = user.getActivePolicy();

    assertThat(result).isNull();
  }

  @Test
  void nullPolicyInsideUsersPolicyTest() {
    UsersPolicy usersPolicy = new UsersPolicy();
    usersPolicy.setPolicy(null);
    usersPolicy.setValidFrom(LocalDate.now().minusDays(5));
    usersPolicy.setValidTo(null);

    user.setUserPolicies(List.of(usersPolicy));

    Policy result = user.getActivePolicy();

    assertThat(result).isNull();
  }

  @Test
  void policyForDateTest() {
    Policy policy = new Policy();
    UsersPolicy usersPolicy = new UsersPolicy();
    usersPolicy.setPolicy(policy);
    usersPolicy.setValidFrom(LocalDate.now().minusDays(5));
    usersPolicy.setValidTo(LocalDate.now().minusDays(3));

    user.setUserPolicies(List.of(usersPolicy));

    Policy result = user.getPolicyForDate(LocalDate.now().minusDays(4));

    assertThat(result).isEqualTo(policy);
  }


  @Test
  void getPolicyForDateNullUserPoliciesTest() {
    user.setUserPolicies(null);

    Policy result = user.getPolicyForDate(LocalDate.now().minusDays(4));

    assertThat(result).isNull();
  }

  @Test
  void getPolicyForDateEmptyUserPoliciesTest() {
    user.setUserPolicies(new ArrayList<>());

    Policy result = user.getPolicyForDate(LocalDate.now().minusDays(4));

    assertThat(result).isNull();
  }

  @Test
  void getPolicyForDateNullDateTest() {
    UsersPolicy up = new UsersPolicy();
    user.setUserPolicies(List.of(up));

    Policy result = user.getPolicyForDate(null);

    assertThat(result).isNull();
  }

  @Test
  void getPolicyForDateValidFromFilterTest() {
    Policy policy1 = new Policy();
    Policy policy2 = new Policy();
    Policy policy3 = new Policy();
    UsersPolicy nullStartTime = new UsersPolicy();
    nullStartTime.setPolicy(policy1);
    nullStartTime.setValidFrom(null);
    UsersPolicy currentPolicy = new UsersPolicy();
    currentPolicy.setPolicy(policy2);
    currentPolicy.setValidFrom(LocalDate.now());
    UsersPolicy pastPolicy = new UsersPolicy();
    pastPolicy.setPolicy(policy3);
    pastPolicy.setValidFrom(LocalDate.now().minusDays(3));
    pastPolicy.setValidTo(LocalDate.now().minusDays(1));
    user.setUserPolicies(List.of(nullStartTime, currentPolicy, pastPolicy));

    assertThat(user.getPolicyForDate(LocalDate.now().minusDays(4))).isNull();
    assertThat(user.getPolicyForDate(LocalDate.now().minusDays(2))).isEqualTo(policy3);
  }

  @Test
  void getPolicyForDatDateGapTest() {
    Policy policy = new Policy();
    UsersPolicy up1 = new UsersPolicy();
    up1.setPolicy(policy);
    up1.setValidFrom(LocalDate.of(2026, 6, 1));
    up1.setValidTo(LocalDate.of(2026, 6, 2));

    UsersPolicy up2 = new UsersPolicy();
    up2.setPolicy(policy);
    up2.setValidFrom(LocalDate.of(2026, 6, 6));
    up2.setValidTo(null);

    user.setUserPolicies(List.of(up1, up2));
    assertThat(user.getPolicyForDate(LocalDate.of(2026, 6, 4))).isNull();
  }

  @Test
  void getPolicyForDateNullValidToFindTest() {
    Policy policy = new Policy();
    UsersPolicy up = new UsersPolicy();
    up.setPolicy(policy);
    up.setValidFrom(LocalDate.of(2026, 6, 1));
    up.setValidTo(null);

    user.setUserPolicies(List.of(up));

    Policy result = user.getPolicyForDate(LocalDate.of(2026, 6, 15));

    assertThat(result).isEqualTo(policy);
  }
}
