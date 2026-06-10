package pl.chrono.dbmanager.model;

import java.time.LocalDate;
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

}
