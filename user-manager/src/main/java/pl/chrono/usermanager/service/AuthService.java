package pl.chrono.usermanager.service;

import java.time.LocalDateTime;
import java.util.UUID;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;
import pl.chrono.dbmanager.model.Role;
import pl.chrono.dbmanager.model.User;
import pl.chrono.dbmanager.ports.I_UserData;
import pl.chrono.logmanager.audit.LogActivity;
import pl.chrono.usermanager.dto.request.AuthRequest;
import pl.chrono.usermanager.dto.request.ChangePasswordRequest;
import pl.chrono.usermanager.dto.request.RegisterRequest;
import pl.chrono.usermanager.dto.response.AuthResponse;
import pl.chrono.usermanager.dto.response.RegisterResponse;
import pl.chrono.usermanager.event.PasswordResetEvent;
import pl.chrono.usermanager.event.UserLoginEvent;
import pl.chrono.usermanager.security.JwtService;
import org.springframework.security.crypto.password.PasswordEncoder;

@Service
@RequiredArgsConstructor
public class AuthService {

  private final I_UserData userData;
  private final PasswordEncoder passwordEncoder;
  private final JwtService jwtService;
  private final AuthenticationManager authenticationManager;
  private final ApplicationEventPublisher eventPublisher;

  public AuthResponse login(AuthRequest request) {
    authenticationManager.authenticate(
        new UsernamePasswordAuthenticationToken(
            request.email(),
            request.password()
        )
    );

    var user = userData.findByEmail(request.email()).orElseThrow(
        () -> new IllegalArgumentException("User not found"));

    eventPublisher.publishEvent(new UserLoginEvent(user.getUserId(), user.getEmail()));

    var jwtToken = jwtService.generateToken(user);

    String roleName = user.getRoles().stream()
        .findFirst()
        .map(Role::getName)
        .orElse("UNKNOWN");

    return new AuthResponse(
        jwtToken,
        roleName,
        user.isForcePasswordChange()
    );
  }

  @Transactional
  @LogActivity(action = "REGISTER_NEW_USER", details = "Successful register")
  public RegisterResponse registerNewUser(RegisterRequest request) {
    if(userData.findByEmail(request.email()).isPresent()) {
      throw new IllegalArgumentException("Użytkownik o podanym adresie email już istnieje.");
    }

    Role role = userData.findRoleByName(request.roleName()).orElseThrow(
        () -> new IllegalArgumentException("Podanan rola nie istnieje: " + request.roleName())
    );

    String tempPassword = UUID.randomUUID().toString().substring(0, 8);

    User newUser = new User();
    newUser.setEmail(request.email());
    newUser.setName(request.name());
    newUser.setSurname(request.surname());
    newUser.setPasswordHash(passwordEncoder.encode(tempPassword));
    newUser.getRoles().add(role);
    newUser.setForcePasswordChange(true);

    if(request.managerEmail() != null && !request.managerEmail().isBlank()) {
      User manager = userData.findByEmail(request.managerEmail())
          .orElseThrow(() -> new IllegalArgumentException("Użytkownik o podanym adresie email nie istnieje."));

      newUser.setManager(manager);
    }

    userData.save(newUser);

    return new RegisterResponse(newUser.getEmail(), tempPassword);
  }

  @Transactional
  public void changeFirstPassword(String userEmail, ChangePasswordRequest request){
    User user = userData.findByEmail(userEmail)
        .orElseThrow( () -> new IllegalArgumentException("Użytkownik o podanym adresie email nie istnieje.")
    );

    if(!user.isForcePasswordChange()) {
      throw new IllegalArgumentException("Zmiana hasła nie jest wymagana dla tego użytkownika");
    }

    user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
    user.setForcePasswordChange(false);
    userData.save(user);
  }

  public Integer extractUserIdFromToken(String token) {
    String email = jwtService.extractUsername(token);
    return userData.findByEmail(email)
        .map(User::getUserId)
        .orElseThrow(() -> new IllegalArgumentException("Nie znaleziono użytkownika dla podanego tokena"));
  }

  @Transactional
  public void requestPasswordReset(String email){
    userData.findByEmail(email).ifPresent(user -> {
      String token = UUID.randomUUID().toString();
      user.setResetToken(token);
      user.setResetTokenExpiration(LocalDateTime.now().plusMinutes(15));
      userData.save(user);

      //To w przyszłości będzie trzeba podmienić na wywołanie logiki wysyłania maili do użytkownika
      eventPublisher.publishEvent(new PasswordResetEvent(user.getUserId(), user.getEmail(), token));
    });
  }

  @Transactional
  @LogActivity(action = "PASSWORD_RESET", details = "User reseted theirs password")
  public void resetPassword(String token, String newPassword) {
    User user = userData.findByResetToken(token)
        .orElseThrow(() -> new IllegalArgumentException("Nieprawidłowy lub wygasły token."));
    if(user.getResetTokenExpiration().isBefore(LocalDateTime.now())){
      throw new IllegalArgumentException("Token do resetu hasła wygasł.");
    }

    user.setPasswordHash(passwordEncoder.encode(newPassword));
    user.setResetToken(null);
    user.setResetTokenExpiration(null);
    user.setForcePasswordChange(false);
    userData.save(user);
  }

  @Transactional
  @LogActivity(action = "PASSWORD_CHANGE", details = "User changed theirs password")
  public void changePassword(String email, ChangePasswordRequest request) {
    User user = userData.findByEmail(email)
        .orElseThrow(() -> new IllegalArgumentException("Użytkownik o podanym adresie email nie istnieje."));

    if(passwordEncoder.matches(request.oldPassword(), user.getPasswordHash())) {
      throw new IllegalArgumentException("Podane aktualne hasło jest niepoprawne.");
    }

    if(passwordEncoder.matches(request.newPassword(), user.getPasswordHash())) {
      throw new IllegalArgumentException("Nowe hasło nie może być takie samo jak obecne.");
    }

    user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
    userData.save(user);
  }
}

