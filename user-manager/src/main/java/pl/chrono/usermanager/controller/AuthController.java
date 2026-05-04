package pl.chrono.usermanager.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pl.chrono.usermanager.dto.request.AuthRequest;
import pl.chrono.usermanager.dto.request.TokenRequest;
import pl.chrono.usermanager.dto.response.AuthResponse;
import pl.chrono.usermanager.service.AuthService;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

  private final AuthService authService;

  public AuthController(AuthService authService) {
    this.authService = authService;
  }

  @PostMapping("/login")
  public ResponseEntity<AuthResponse> login(@RequestBody AuthRequest request) {
    return ResponseEntity.ok(authService.login(request));
  }

  @PostMapping("/extract-user-id")
  public ResponseEntity<Integer> extractUserIdFromToken(@RequestBody TokenRequest request) {
    return ResponseEntity.ok(authService.extractUserIdFromToken(request.token()));
  }
}
