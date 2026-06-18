package pl.chrono.usermanager.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import pl.chrono.dbmanager.model.Role;
import pl.chrono.dbmanager.model.User;
import pl.chrono.dbmanager.repository.RoleRepository;
import pl.chrono.dbmanager.repository.UserRepository;
import pl.chrono.usermanager.TestConfig;
import pl.chrono.usermanager.dto.request.AuthRequest;
import pl.chrono.usermanager.dto.request.ChangePasswordRequest;
import pl.chrono.usermanager.dto.request.RegisterRequest;
import pl.chrono.usermanager.dto.response.AuthResponse;
import pl.chrono.usermanager.dto.response.RegisterResponse;

import java.time.LocalDateTime;
import java.util.UUID;

@SpringBootTest(
    classes = TestConfig.class,
    properties = {
        "application.security.jwt.secret-key=bardzotajnyklucztestowy12345678901234567890",
        "application.security.jwt.expiration=86400000"
    }
)
@Transactional
@ActiveProfiles("local")
class AuthServiceIT {

  @Autowired
  private AuthService authService;

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private RoleRepository roleRepository;

  @Autowired
  private PasswordEncoder passwordEncoder;

  @Autowired
  private EntityManager entityManager;

  private Role employeeRole;

  @BeforeEach
  void setUp() {
    userRepository.deleteAll();

    employeeRole = roleRepository.findByName("ROLE_EMPLOYEE")
        .orElseGet(() -> {
          Role role = new Role();
          role.setName("ROLE_EMPLOYEE");
          return roleRepository.save(role);
        });
  }

  @Test
  void registerNewUser_savesUserAndReturnsTempPassword() {
    RegisterRequest request = new RegisterRequest(
        "Jan", "Kowalski", "nowy@test.pl", "ROLE_EMPLOYEE", null
    );

    RegisterResponse response = authService.registerNewUser(request);

    assertThat(response.email()).isEqualTo("nowy@test.pl");
    assertThat(response.temporaryPassword()).isNotBlank().hasSize(8); 

    User savedUser = userRepository.findByEmail("nowy@test.pl").orElseThrow();
    assertThat(savedUser.getName()).isEqualTo("Jan");
    assertThat(savedUser.getSurname()).isEqualTo("Kowalski");
    assertThat(savedUser.isForcePasswordChange()).isTrue();
    assertThat(passwordEncoder.matches(response.temporaryPassword(), savedUser.getPasswordHash())).isTrue();
  }

  @Test
  void registerNewUser_throwsException_whenEmailAlreadyExists() {
    persistUser("istnieje@test.pl", "haslo123", employeeRole, false);
    RegisterRequest request = new RegisterRequest(
        "Adam", "Nowak", "istnieje@test.pl", "ROLE_EMPLOYEE", null
    );

    assertThatThrownBy(() -> authService.registerNewUser(request))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Użytkownik o podanym adresie email już istnieje.");
  }

  @Test
  void login_throwsException_whenCredentialsAreInvalid() {
    persistUser("login@test.pl", "PrawidloweHaslo1", employeeRole, false);
    AuthRequest request = new AuthRequest("login@test.pl", "ZleHaslo!");

    assertThatThrownBy(() -> authService.login(request))
        .isInstanceOf(AuthenticationException.class);
  }

  @Test
  void changeFirstPassword_updatesPasswordAndRemovesForceChangeFlag() {
    persistUser("zmiana@test.pl", "temp123", employeeRole, true);
    ChangePasswordRequest request = new ChangePasswordRequest("NoweHaslo123!", null);

    authService.changeFirstPassword("zmiana@test.pl", request);

    entityManager.flush();
    entityManager.clear();

    User updatedUser = userRepository.findByEmail("zmiana@test.pl").orElseThrow();
    assertThat(updatedUser.isForcePasswordChange()).isFalse();
    assertThat(passwordEncoder.matches("NoweHaslo123!", updatedUser.getPasswordHash())).isTrue();
  }

  @Test
  void changeFirstPassword_throwsException_whenNotRequired() {
    persistUser("brakwymogu@test.pl", "haslo123", employeeRole, false);
    ChangePasswordRequest request = new ChangePasswordRequest("NoweHaslo123!", null);

    assertThatThrownBy(() -> authService.changeFirstPassword("brakwymogu@test.pl", request))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Zmiana hasła nie jest wymagana dla tego użytkownika");
  }

  @Test
  void requestPasswordReset_generatesResetToken() {
    persistUser("reset@test.pl", "haslo", employeeRole, false);

    authService.requestPasswordReset("reset@test.pl");

    entityManager.flush();
    entityManager.clear();

    User updatedUser = userRepository.findByEmail("reset@test.pl").orElseThrow();
    assertThat(updatedUser.getResetToken()).isNotNull();
    assertThat(updatedUser.getResetTokenExpiration()).isAfter(LocalDateTime.now());
  }

  @Test
  void resetPassword_updatesPasswordAndClearsToken() {
    User user = persistUser("doResetu@test.pl", "stare", employeeRole, false);
    String token = UUID.randomUUID().toString();
    user.setResetToken(token);
    user.setResetTokenExpiration(LocalDateTime.now().plusMinutes(10));
    userRepository.save(user);

    authService.resetPassword(token, "ZupelnieNoweHaslo1!");

    entityManager.flush();
    entityManager.clear();

    User updatedUser = userRepository.findByEmail("doResetu@test.pl").orElseThrow();
    assertThat(updatedUser.getResetToken()).isNull();
    assertThat(updatedUser.getResetTokenExpiration()).isNull();
    assertThat(passwordEncoder.matches("ZupelnieNoweHaslo1!", updatedUser.getPasswordHash())).isTrue();
  }

  @Test
  void resetPassword_throwsException_whenTokenExpired() {
    User user = persistUser("wygasly@test.pl", "stare", employeeRole, false);
    String token = UUID.randomUUID().toString();
    user.setResetToken(token);
    user.setResetTokenExpiration(LocalDateTime.now().minusMinutes(5));
    userRepository.save(user);

    assertThatThrownBy(() -> authService.resetPassword(token, "NoweHaslo1!"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Token do resetu hasła wygasł.");
  }

  @Test
  void changePassword_updatesPassword_whenOldIsCorrect() {
    persistUser("zmiana2@test.pl", "StareHaslo1", employeeRole, false);
    ChangePasswordRequest request = new ChangePasswordRequest("NoweHaslo2", "StareHaslo1");

    authService.changePassword("zmiana2@test.pl", request);

    entityManager.flush();
    entityManager.clear();

    User updatedUser = userRepository.findByEmail("zmiana2@test.pl").orElseThrow();
    assertThat(passwordEncoder.matches("NoweHaslo2", updatedUser.getPasswordHash())).isTrue();
  }

  @Test
  void changePassword_throwsException_whenOldIsIncorrect() {
    persistUser("zmiana2@test.pl", "StareHaslo1", employeeRole, false);
    ChangePasswordRequest request = new ChangePasswordRequest("NoweHaslo2", "ZleStareHaslo");

    assertThatThrownBy(() -> authService.changePassword("zmiana2@test.pl", request))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Podane aktualne hasło jest niepoprawne.");
  }

  private User persistUser(String email, String plainPassword, Role role, boolean forceChange) {
    User user = new User();
    user.setName("Test");
    user.setSurname("User");
    user.setEmail(email);
    user.setPasswordHash(passwordEncoder.encode(plainPassword));
    user.setForcePasswordChange(forceChange);
    user.getRoles().add(role);
    return userRepository.save(user);
  }
}
