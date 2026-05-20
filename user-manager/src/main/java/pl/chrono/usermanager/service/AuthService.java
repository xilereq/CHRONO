package pl.chrono.usermanager.service;

import java.time.LocalDateTime;
import java.util.UUID;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;
import pl.chrono.dbmanager.model.Role;
import pl.chrono.dbmanager.model.User;
import pl.chrono.dbmanager.repository.RoleRepository;
import pl.chrono.dbmanager.repository.UserRepository;
import pl.chrono.logmanager.audit.LogActivity;
import pl.chrono.usermanager.dto.request.AuthRequest;
import pl.chrono.usermanager.dto.request.ChangePasswordRequest;
import pl.chrono.usermanager.dto.request.RegisterRequest;
import pl.chrono.usermanager.dto.response.AuthResponse;
import pl.chrono.usermanager.dto.response.RegisterResponse;
import pl.chrono.usermanager.security.JwtService;
import org.springframework.security.crypto.password.PasswordEncoder;

@Service
@RequiredArgsConstructor
public class AuthService {

  private final UserRepository userRepository;
  private final RoleRepository roleRepository;
  private final PasswordEncoder passwordEncoder;
  private final JwtService jwtService;
  private final AuthenticationManager authenticationManager;

  public AuthResponse login(AuthRequest request) {
    authenticationManager.authenticate(
        new UsernamePasswordAuthenticationToken(
            request.email(),
            request.password()
        )
    );

    var user = userRepository.findByEmail(request.email()).orElseThrow(
        () -> new IllegalArgumentException("User not found"));

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
    if(userRepository.findByEmail(request.email()).isPresent()) {
      throw new IllegalArgumentException("Użytkownik o podanym adresie email już istnieje.");
    }

    Role role = roleRepository.findByName(request.roleName()).orElseThrow(
        () -> new IllegalArgumentException("Podanan rola nie istnieje: " + request.roleName())
    );

    String tempPassword = UUID.randomUUID().toString().substring(0, 8);

    User newUser = new User();
    newUser.setEmail(request.email());
    newUser.setPasswordHash(passwordEncoder.encode(tempPassword));
    newUser.getRoles().add(role);
    newUser.setForcePasswordChange(true);

    userRepository.save(newUser);

    return new RegisterResponse(newUser.getEmail(), tempPassword);
  }

  @Transactional
  public void changeFirstPassword(String userEmail, ChangePasswordRequest request){
    User user = userRepository.findByEmail(userEmail).orElseThrow(
        () -> new IllegalArgumentException("Nie znaleziono użytkownika")
    );

    if(!user.isForcePasswordChange()) {
      throw new IllegalArgumentException("Zmiana hasła nie jest wymagana dla tego użytkownika");
    }

    user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
    user.setForcePasswordChange(false);
    userRepository.save(user);
  }

  public Integer extractUserIdFromToken(String token) {
    String email = jwtService.extractUsername(token);
    return userRepository.findByEmail(email)
        .map(User::getUserId)
        .orElseThrow(() -> new IllegalArgumentException("Nie znaleziono użytkownika dla podanego tokena"));
  }

  @Transactional
  public void requestPasswordRest(String email){
    userRepository.findByEmail(email).ifPresent(user -> {
      String token = UUID.randomUUID().toString();
      user.setResetToken(token);
      user.setResetTokenExpiration(LocalDateTime.now().plusMinutes(15));
      userRepository.save(user);

      //To w przyszłości będzie trzeba podmienić na wywołanie logiki wysyłania maili do użytkownika
      System.out.println("LINK DO RESETU HASŁA: http://localhost:3000/reset-password?token=" + token);
    });
  }

  @Transactional
  public void ressetPassword(String token, String newPassword) {
    User user = userRepository.findByResetToken(token)
        .orElseThrow(() -> new IllegalArgumentException("Nieprawidłowy lub wygasły token."));
    if(user.getResetTokenExpiration().isBefore(LocalDateTime.now())){
      throw new IllegalArgumentException("Token do resetu hasła wygasł.");
    }

    user.setPasswordHash(passwordEncoder.encode(newPassword));
    user.setResetToken(null);
    user.setResetTokenExpiration(null);
    user.setForcePasswordChange(false);
    userRepository.save(user);
  }
}

