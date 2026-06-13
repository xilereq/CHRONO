package pl.chrono.usermanager.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import pl.chrono.dbmanager.model.Role;
import pl.chrono.dbmanager.model.User;
import pl.chrono.dbmanager.ports.I_UserData;
import pl.chrono.usermanager.dto.request.AuthRequest;
import pl.chrono.usermanager.dto.request.ChangePasswordRequest;
import pl.chrono.usermanager.dto.request.RegisterRequest;
import pl.chrono.usermanager.dto.response.AuthResponse;
import pl.chrono.usermanager.dto.response.RegisterResponse;
import pl.chrono.usermanager.event.PasswordResetEvent;
import pl.chrono.usermanager.event.UserLoginEvent;
import pl.chrono.usermanager.security.JwtService;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

  @Mock private I_UserData userData;
  @Mock private PasswordEncoder passwordEncoder;
  @Mock private JwtService jwtService;
  @Mock private AuthenticationManager authenticationManager;
  @Mock private ApplicationEventPublisher eventPublisher;

  @InjectMocks
  private AuthService authService;

  @Test
  void login_ShouldReturnAuthResponse_WhenCredentialsAreValid() {
    AuthRequest request = new AuthRequest("test@test.pl", "password");
    User user = new User();
    user.setUserId(1);
    user.setEmail("test@test.pl");
    user.setForcePasswordChange(true);
    Role role = new Role();
    role.setName("ROLE_ADMIN");
    user.getRoles().add(role);

    when(userData.findByEmail(request.email())).thenReturn(Optional.of(user));
    when(jwtService.generateToken(user)).thenReturn("mocked_jwt_token");

    AuthResponse response = authService.login(request);

    assertNotNull(response);
    assertEquals("mocked_jwt_token", response.token());
    assertEquals("ROLE_ADMIN", response.role());
    assertTrue(response.forcePasswordChange());

    verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
    verify(eventPublisher).publishEvent(any(UserLoginEvent.class));
  }

  @Test
  void login_ShouldReturnUnknownRole_WhenUserHasNoRoles() {
    AuthRequest request = new AuthRequest("test@test.pl", "password");
    User user = new User();
    user.setEmail("test@test.pl");

    when(userData.findByEmail(request.email())).thenReturn(Optional.of(user));
    when(jwtService.generateToken(user)).thenReturn("mocked_jwt_token");

    AuthResponse response = authService.login(request);

    assertEquals("UNKNOWN", response.role());
  }

  @Test
  void login_ShouldThrowException_WhenUserNotFound() {
    AuthRequest request = new AuthRequest("nonexistent@test.pl", "password");
    when(userData.findByEmail(request.email())).thenReturn(Optional.empty());

    IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> authService.login(request));
    assertEquals("User not found", ex.getMessage());
  }

  @Test
  void registerNewUser_ShouldThrowException_WhenUserAlreadyExists() {
    RegisterRequest request = new RegisterRequest("Jan", "Kowalski", "exist@test.pl", "ROLE_EMPLOYEE", null);
    when(userData.findByEmail(request.email())).thenReturn(Optional.of(new User()));

    IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> authService.registerNewUser(request));
    assertEquals("Użytkownik o podanym adresie email już istnieje.", ex.getMessage());
  }

  @Test
  void registerNewUser_ShouldThrowException_WhenRoleNotFound() {
    RegisterRequest request = new RegisterRequest("Jan", "Kowalski", "new@test.pl", "BAD_ROLE", null);
    when(userData.findByEmail(request.email())).thenReturn(Optional.empty());
    when(userData.findRoleByName(request.roleName())).thenReturn(Optional.empty());

    IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> authService.registerNewUser(request));
    assertTrue(ex.getMessage().contains("Podanan rola nie istnieje"));
  }

  @Test
  void registerNewUser_ShouldRegisterSuccessfully_WithoutManager() {
    RegisterRequest request = new RegisterRequest("Jan", "Kowalski", "new@test.pl", "ROLE_EMPLOYEE", null);
    Role role = new Role();
    role.setName("ROLE_EMPLOYEE");

    when(userData.findByEmail(request.email())).thenReturn(Optional.empty());
    when(userData.findRoleByName(request.roleName())).thenReturn(Optional.of(role));
    when(passwordEncoder.encode(anyString())).thenReturn("hashed_password");

    RegisterResponse response = authService.registerNewUser(request);

    assertNotNull(response);
    assertEquals("new@test.pl", response.email());
    verify(userData).save(any(User.class));
  }

  @Test
  void registerNewUser_ShouldRegisterSuccessfully_WithManager() {
    RegisterRequest request = new RegisterRequest("Jan", "Kowalski", "new@test.pl", "ROLE_EMPLOYEE", "manager@test.pl");
    Role role = new Role(); role.setName("ROLE_EMPLOYEE");
    User manager = new User(); manager.setEmail("manager@test.pl");

    when(userData.findByEmail("new@test.pl")).thenReturn(Optional.empty());
    when(userData.findRoleByName("ROLE_EMPLOYEE")).thenReturn(Optional.of(role));
    when(userData.findByEmail("manager@test.pl")).thenReturn(Optional.of(manager));
    when(passwordEncoder.encode(anyString())).thenReturn("hashed_password");

    authService.registerNewUser(request);

    ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
    verify(userData).save(userCaptor.capture());
    assertEquals(manager, userCaptor.getValue().getManager());
  }

  @Test
  void registerNewUser_ShouldThrowException_WhenManagerNotFound() {
    RegisterRequest request = new RegisterRequest("Jan", "Kowalski", "new@test.pl", "ROLE_EMPLOYEE", "badmanager@test.pl");
    Role role = new Role(); role.setName("ROLE_EMPLOYEE");

    when(userData.findByEmail("new@test.pl")).thenReturn(Optional.empty());
    when(userData.findRoleByName("ROLE_EMPLOYEE")).thenReturn(Optional.of(role));
    when(userData.findByEmail("badmanager@test.pl")).thenReturn(Optional.empty());

    IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> authService.registerNewUser(request));
    assertEquals("Użytkownik o podanym adresie email nie istnieje.", ex.getMessage());
  }

  @Test
  void changeFirstPassword_ShouldChangePasswordSuccessfully() {
    // POPRAWKA: ChangePasswordRequest(newPassword, oldPassword)
    ChangePasswordRequest request = new ChangePasswordRequest("newPassword", "ignored");
    User user = new User();
    user.setForcePasswordChange(true);

    when(userData.findByEmail("test@test.pl")).thenReturn(Optional.of(user));
    when(passwordEncoder.encode("newPassword")).thenReturn("newHashedPassword");

    authService.changeFirstPassword("test@test.pl", request);

    assertEquals("newHashedPassword", user.getPasswordHash());
    assertFalse(user.isForcePasswordChange());
    verify(userData).save(user);
  }

  @Test
  void changeFirstPassword_ShouldThrowException_WhenUserNotFound() {
    when(userData.findByEmail(anyString())).thenReturn(Optional.empty());
    assertThrows(IllegalArgumentException.class, () -> authService.changeFirstPassword("x@x.pl", null));
  }

  @Test
  void changeFirstPassword_ShouldThrowException_WhenChangeNotRequired() {
    User user = new User();
    user.setForcePasswordChange(false);
    when(userData.findByEmail("test@test.pl")).thenReturn(Optional.of(user));

    IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> authService.changeFirstPassword("test@test.pl", null));
    assertEquals("Zmiana hasła nie jest wymagana dla tego użytkownika", ex.getMessage());
  }

  @Test
  void extractUserIdFromToken_ShouldReturnId() {
    User user = new User(); user.setUserId(99);
    when(jwtService.extractUsername("token123")).thenReturn("test@test.pl");
    when(userData.findByEmail("test@test.pl")).thenReturn(Optional.of(user));

    Integer id = authService.extractUserIdFromToken("token123");
    assertEquals(99, id);
  }

  @Test
  void extractUserIdFromToken_ShouldThrowException_WhenUserNotFound() {
    when(jwtService.extractUsername("token123")).thenReturn("test@test.pl");
    when(userData.findByEmail("test@test.pl")).thenReturn(Optional.empty());

    assertThrows(IllegalArgumentException.class, () -> authService.extractUserIdFromToken("token123"));
  }

  @Test
  void requestPasswordReset_ShouldSetTokenAndPublishEvent_WhenUserExists() {
    User user = new User(); user.setEmail("test@test.pl"); user.setUserId(1);
    when(userData.findByEmail("test@test.pl")).thenReturn(Optional.of(user));

    authService.requestPasswordReset("test@test.pl");

    assertNotNull(user.getResetToken());
    assertNotNull(user.getResetTokenExpiration());
    verify(userData).save(user);
    verify(eventPublisher).publishEvent(any(PasswordResetEvent.class));
  }

  @Test
  void requestPasswordReset_ShouldDoNothing_WhenUserAbsent() {
    when(userData.findByEmail("bad@test.pl")).thenReturn(Optional.empty());
    authService.requestPasswordReset("bad@test.pl");
    verify(userData, never()).save(any());
    verify(eventPublisher, never()).publishEvent(any());
  }

  @Test
  void resetPassword_ShouldUpdatePassword_WhenTokenIsValid() {
    User user = new User();
    user.setResetToken("token");
    user.setResetTokenExpiration(LocalDateTime.now().plusMinutes(10));
    when(userData.findByResetToken("token")).thenReturn(Optional.of(user));
    when(passwordEncoder.encode("newPass")).thenReturn("hashedPass");

    authService.resetPassword("token", "newPass");

    assertEquals("hashedPass", user.getPasswordHash());
    assertNull(user.getResetToken());
    assertNull(user.getResetTokenExpiration());
    assertFalse(user.isForcePasswordChange());
    verify(userData).save(user);
  }

  @Test
  void resetPassword_ShouldThrowException_WhenTokenInvalid() {
    when(userData.findByResetToken("badToken")).thenReturn(Optional.empty());
    assertThrows(IllegalArgumentException.class, () -> authService.resetPassword("badToken", "pass"));
  }

  @Test
  void resetPassword_ShouldThrowException_WhenTokenExpired() {
    User user = new User();
    user.setResetTokenExpiration(LocalDateTime.now().minusMinutes(5));
    when(userData.findByResetToken("token")).thenReturn(Optional.of(user));

    IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> authService.resetPassword("token", "pass"));
    assertEquals("Token do resetu hasła wygasł.", ex.getMessage());
  }

  @Test
  void changePassword_ShouldUpdatePasswordSuccessfully() {
    ChangePasswordRequest request = new ChangePasswordRequest("newPass", "oldPass");
    User user = new User(); user.setPasswordHash("hashedOld");

    when(userData.findByEmail("test@test.pl")).thenReturn(Optional.of(user));
    when(passwordEncoder.matches("oldPass", "hashedOld")).thenReturn(true);
    when(passwordEncoder.matches("newPass", "hashedOld")).thenReturn(false);
    when(passwordEncoder.encode("newPass")).thenReturn("hashedNew");

    authService.changePassword("test@test.pl", request);

    assertEquals("hashedNew", user.getPasswordHash());
    verify(userData).save(user);
  }

  @Test
  void changePassword_ShouldThrowException_WhenUserNotFound() {
    when(userData.findByEmail("test@test.pl")).thenReturn(Optional.empty());
    assertThrows(IllegalArgumentException.class, () -> authService.changePassword("test@test.pl", null));
  }

  @Test
  void changePassword_ShouldThrowException_WhenOldPasswordIsIncorrect() {
    ChangePasswordRequest request = new ChangePasswordRequest("newPass", "wrongOld");
    User user = new User(); user.setPasswordHash("hashedOld");

    when(userData.findByEmail("test@test.pl")).thenReturn(Optional.of(user));
    when(passwordEncoder.matches("wrongOld", "hashedOld")).thenReturn(false);

    IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> authService.changePassword("test@test.pl", request));
    assertEquals("Podane aktualne hasło jest niepoprawne.", ex.getMessage());
  }

  @Test
  void changePassword_ShouldThrowException_WhenNewPasswordIsSameAsOld() {
    ChangePasswordRequest request = new ChangePasswordRequest("oldPass", "oldPass");
    User user = new User(); user.setPasswordHash("hashedOld");

    when(userData.findByEmail("test@test.pl")).thenReturn(Optional.of(user));
    when(passwordEncoder.matches("oldPass", "hashedOld")).thenReturn(true);

    IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> authService.changePassword("test@test.pl", request));
    assertEquals("Nowe hasło nie może być takie samo jak obecne.", ex.getMessage());
  }
}
