package pl.chrono.dbmanager.repository;

import java.time.LocalDate;
import java.util.Optional;
import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import pl.chrono.dbmanager.model.Policy;
import pl.chrono.dbmanager.model.User;
import pl.chrono.dbmanager.model.UsersPolicy;

@DataJpaTest
class UsersPolicyRepositoryIT {

  @Autowired
  private TestEntityManager em;

  @Autowired
  private UsersPolicyRepository repository;

  private User user;
  private Policy policy;

  @BeforeEach
  void setUp() {
    user = new User();
    user.setName("Jan");
    user.setSurname("Kowalski");
    user.setEmail("jan.kowalski@test.com");
    user.setPasswordHash("hash");
    user = em.persist(user);

    policy = new Policy();
    policy.setName("Standardowa polityka");
    policy.setDailyHours(8);
    policy = em.persist(policy);

    em.flush();
  }

  @Test
  void activePolicyFindTest() {
    UsersPolicy activePolicy = new UsersPolicy();
    activePolicy.setUser(user);
    activePolicy.setPolicy(policy);
    activePolicy.setValidFrom(LocalDate.now().minusDays(10));
    activePolicy.setValidTo(null);
    em.persistAndFlush(activePolicy);

    Optional<UsersPolicy> result = repository.findActiveByUserId(user.getUserId());

    assertThat(result).isPresent();
    assertThat(result.get().getValidTo()).isNull();
  }

  @Test
  void expiredPolicyFindTest() {
    UsersPolicy expiredPolicy = new UsersPolicy();
    expiredPolicy.setUser(user);
    expiredPolicy.setPolicy(policy);
    expiredPolicy.setValidFrom(LocalDate.now().minusDays(30));
    expiredPolicy.setValidTo(LocalDate.now().minusDays(5));
    em.persistAndFlush(expiredPolicy);

    Optional<UsersPolicy> result = repository.findActiveByUserId(user.getUserId());

    assertThat(result).isEmpty();
  }
}
