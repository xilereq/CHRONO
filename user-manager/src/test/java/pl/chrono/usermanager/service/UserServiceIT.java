package pl.chrono.usermanager.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import pl.chrono.dbmanager.model.Role;
import pl.chrono.dbmanager.model.User;
import pl.chrono.dbmanager.repository.RoleRepository;
import pl.chrono.dbmanager.repository.UserRepository;
import pl.chrono.usermanager.TestConfig;
import pl.chrono.usermanager.dto.response.UserDto;

import java.util.List;

@SpringBootTest(
    classes = TestConfig.class,
    properties = {
        "application.security.jwt.secret-key=bardzotajnyklucztestowy12345678901234567890",
        "application.security.jwt.expiration=86400000"
    }
)
@Transactional
class UserServiceIT {

  @Autowired
  private UserService userService;

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private RoleRepository roleRepository;

  @Autowired
  private EntityManager entityManager;

  private Role employeeRole;
  private Role managerRole;

  @BeforeEach
  void setUp() {
    userRepository.deleteAll();

    employeeRole = persistRole("ROLE_EMPLOYEE");
    managerRole = persistRole("ROLE_MANAGER");
  }

  @Test
  void getAllEmployees_returnsOnlyUsersWithEmployeeRole() {
    persistUser("Jan", "Kowalski", "jan.kowalski@test.pl", employeeRole, null);
    persistUser("Piotr", "Nowak", "piotr.nowak@test.pl", employeeRole, null);
    persistUser("Anna", "Zarzadca", "anna.zarzadca@test.pl", managerRole, null);

    List<UserDto> result = userService.getAllEmployees();

    assertThat(result).hasSize(2);
    assertThat(result)
        .extracting(UserDto::email)
        .containsExactlyInAnyOrder("jan.kowalski@test.pl", "piotr.nowak@test.pl");
  }

  @Test
  void getSubordinatesForManager_returnsCorrectUsers() {
    User manager = persistUser("Anna", "Zarzadca", "anna.zarzadca@test.pl", managerRole, null);

    persistUser("Jan", "Kowalski", "jan.kowalski@test.pl", employeeRole, manager);
    persistUser("Piotr", "Nowak", "piotr.nowak@test.pl", employeeRole, manager);

    User otherManager = persistUser("Tomasz", "Inny", "tomasz@test.pl", managerRole, null);
    persistUser("Kamil", "InnyPracownik", "kamil@test.pl", employeeRole, otherManager);

    List<UserDto> result = userService.getSubordinatesForManager(manager.getUserId());

    assertThat(result).hasSize(2);
    assertThat(result)
        .extracting(UserDto::email)
        .containsExactlyInAnyOrder("jan.kowalski@test.pl", "piotr.nowak@test.pl");
  }

  @Test
  void setManager_updatesUserManagerSuccessfully() {
    User employee = persistUser("Jan", "Kowalski", "jan.kowalski@test.pl", employeeRole, null);
    User manager = persistUser("Anna", "Zarzadca", "anna.zarzadca@test.pl", managerRole, null);

    entityManager.flush();

    userService.setManager("jan.kowalski@test.pl", "anna.zarzadca@test.pl");

    entityManager.flush();
    entityManager.clear();

    User updatedEmployee = userRepository.findByEmail("jan.kowalski@test.pl").orElseThrow();
    assertThat(updatedEmployee.getManager()).isNotNull();
    assertThat(updatedEmployee.getManager().getUserId()).isEqualTo(manager.getUserId());
  }

  @Test
  void setManager_throwsException_whenEmployeeDoesNotExist() {
    persistUser("Anna", "Zarzadca", "anna.zarzadca@test.pl", managerRole, null);

    assertThatThrownBy(() -> userService.setManager("nieistnieje@test.pl", "anna.zarzadca@test.pl"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Użytkownik o podanym adresie email nie istnieje.");
  }

  @Test
  void setManager_throwsException_whenManagerDoesNotExist() {
    persistUser("Jan", "Kowalski", "jan.kowalski@test.pl", employeeRole, null);

    assertThatThrownBy(() -> userService.setManager("jan.kowalski@test.pl", "brak.managera@test.pl"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Użytkownik o podanym adresie email nie istnieje.");
  }

  private Role persistRole(String name) {
    return roleRepository.findByName(name)
        .orElseGet(() -> {
          Role role = new Role();
          role.setName(name);
          return roleRepository.save(role);
        });
  }

  private User persistUser(String name, String surname, String email, Role role, User manager) {
    User user = new User();
    user.setName(name);
    user.setSurname(surname);
    user.setEmail(email);
    user.setPasswordHash("hashedPassword123");
    user.setForcePasswordChange(false);
    user.getRoles().add(role);

    if (manager != null) {
      user.setManager(manager);
    }

    return userRepository.save(user);
  }
}
