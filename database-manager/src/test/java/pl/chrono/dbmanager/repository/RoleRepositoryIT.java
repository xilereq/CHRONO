package pl.chrono.dbmanager.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import pl.chrono.dbmanager.model.Role;

/**
 * Integration test for {@link RoleRepository}.
 * Uses a real H2 in-memory database (via @DataJpaTest) to verify query behaviour.
 */
@DataJpaTest
class RoleRepositoryIT {

  @Autowired
  private TestEntityManager em;

  @Autowired
  private RoleRepository repository;

  @Test
  void findByName_returnsMatchingRole() {
    Role role = persistRole("ROLE_MANAGER");
    persistRole("ROLE_EMPLOYEE");
    em.flush();

    Optional<Role> result = repository.findByName("ROLE_MANAGER");

    assertThat(result).isPresent();
    assertThat(result.get().getId()).isEqualTo(role.getId());
  }

  @Test
  void findByName_emptyWhenRoleDoesNotExist() {
    persistRole("ROLE_EMPLOYEE");
    em.flush();

    Optional<Role> result = repository.findByName("ROLE_ADMIN");

    assertThat(result).isEmpty();
  }

  @Test
  void findByName_isCaseSensitive() {
    persistRole("ROLE_HR");
    em.flush();

    Optional<Role> result = repository.findByName("role_hr");

    assertThat(result).isEmpty();
  }

  private Role persistRole(String name) {
    Role role = new Role();
    role.setName(name);
    return em.persist(role);
  }
}
