package pl.chrono.dbmanager.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import pl.chrono.dbmanager.model.Role;
import pl.chrono.dbmanager.model.User;

@DataJpaTest
class UserRepositoryIT {

  @Autowired
  private TestEntityManager em;

  @Autowired
  private UserRepository repository;

  private Role adminRole;
  private Role userRole;

  @BeforeEach
  void setUp() {
    adminRole = new Role();
    adminRole.setName("ROLE_ADMIN");
    em.persist(adminRole);

    userRole = new Role();
    userRole.setName("ROLE_USER");
    em.persist(userRole);
  }

  @Test
  void findByEmailTest() {
    User user = persistUser("test@chrono.pl", true, null);

    Optional<User> found = repository.findByEmail("test@chrono.pl");

    assertThat(found).isPresent().get().isEqualTo(user);
  }

  @Test
  void findByRolesNameTest() {
    User admin = persistUser("admin@chrono.pl", true, null);
    admin.setRoles(Set.of(adminRole));

    User standardUser = persistUser("user@chrono.pl", true, null);
    standardUser.setRoles(Set.of(userRole));

    em.flush();

    List<User> admins = repository.findByRoles_Name("ROLE_ADMIN");

    assertThat(admins).hasSize(1).containsExactly(admin);
  }

  @Test
  void findAllByManagerUserIdTest() {
    User manager = persistUser("manager@chrono.pl", true, null);
    User emp1 = persistUser("emp1@chrono.pl", true, manager);
    User emp2 = persistUser("emp2@chrono.pl", true, manager);
    User other = persistUser("other@chrono.pl", true, null);
    em.flush();

    List<User> subordinates = repository.findAllByManager_UserId(manager.getUserId());

    assertThat(subordinates).hasSize(2).containsExactlyInAnyOrder(emp1, emp2);
  }

  @Test
  void findByIsActiveFalseAndEmploymentEndDateBeforeTest() {
    LocalDate cutoffDate = LocalDate.now().minusYears(10);

    User targetUser = persistUser("target@chrono.pl", false, null);
    targetUser.setEmploymentEndDate(cutoffDate.minusDays(1));

    User recentFired = persistUser("recent@chrono.pl", false, null);
    recentFired.setEmploymentEndDate(LocalDate.now().minusYears(5));

    User activeOld = persistUser("active@chrono.pl", true, null);
    activeOld.setEmploymentEndDate(cutoffDate.minusDays(10));

    em.flush();

    List<User> usersToAnonymize = repository.findByIsActiveFalseAndEmploymentEndDateBefore(cutoffDate);

    assertThat(usersToAnonymize).hasSize(1).containsExactly(targetUser);
  }

  @Test
  void findAllUserIdsTest() {
    persistUser("u1@chrono.pl", true, null);
    persistUser("u2@chrono.pl", false, null);
    em.flush();

    List<Integer> ids = repository.findAllUserIds();

    assertThat(ids).hasSize(2).doesNotContainNull();
  }

  private User persistUser(String email, boolean isActive, User manager) {
    User user = new User();
    user.setEmail(email);
    user.setPasswordHash("hash");
    user.setName("Test");
    user.setSurname("User");
    user.setActive(isActive);
    user.setManager(manager);
    return em.persist(user);
  }
}
