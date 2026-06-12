package pl.chrono.workforceapi.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import pl.chrono.usermanager.dto.request.AuthRequest;
import pl.chrono.usermanager.dto.request.ChangePasswordRequest;
import pl.chrono.usermanager.dto.request.RegisterRequest;
import pl.chrono.usermanager.dto.request.TokenRequest;
import pl.chrono.usermanager.dto.response.AuthResponse;
import pl.chrono.usermanager.dto.response.RegisterResponse;
import pl.chrono.usermanager.service.AuthService;
import pl.chrono.usermanager.dto.request.PasswordResetRequest;
import pl.chrono.usermanager.dto.request.PasswordResetConfirmRequest;
import java.security.Principal;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

  private final AuthService authService;

  public AuthController(AuthService authService) {
    this.authService = authService;
  }

  @PostMapping("/login")
  public ResponseEntity<AuthResponse> login(@RequestBody AuthRequest request,
                                            @RequestHeader(value = "User-Agent", required = false) String userAgent) {
    ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
    if(attributes != null && userAgent != null){
      attributes.setAttribute("Saved-User-Agent", userAgent, RequestAttributes.SCOPE_REQUEST);
    }
    return ResponseEntity.ok(authService.login(request));
  }

  @PostMapping("/extract-user-id")
  public ResponseEntity<Integer> extractUserIdFromToken(@RequestBody TokenRequest request) {
    return ResponseEntity.ok(authService.extractUserIdFromToken(request.token()));
  }

  @PostMapping("/register")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<RegisterResponse> register(@RequestBody RegisterRequest request) {
    return ResponseEntity.ok(authService.registerNewUser(request));
  }

  @PostMapping("/change-first-password")
  public ResponseEntity<RegisterResponse> changeFirstPassword(@RequestBody ChangePasswordRequest request, Authentication authentication){
    String userEmail = authentication.getName();
    authService.changeFirstPassword(userEmail, request);
    return ResponseEntity.ok().build();
  }

  @PostMapping("/forgot-password")
  public ResponseEntity<?> forgotPassword(@RequestBody PasswordResetRequest request) {
    authService.requestPasswordReset(request.email());
    return ResponseEntity.ok().build();
  }

  @PostMapping("/reset-password")
  public ResponseEntity<?> resetPassword(@RequestBody PasswordResetConfirmRequest request) {
    authService.resetPassword(request.token(), request.password());
    return ResponseEntity.ok().build();
  }

  @PostMapping("/change-password")
  @PreAuthorize("isAuthenticated()")
  public ResponseEntity<?> changePassword(@RequestBody ChangePasswordRequest request, Principal principal){
    String userEmail = principal.getName();

    authService.changePassword(userEmail, request);

    return ResponseEntity.ok().build();
  }
}
