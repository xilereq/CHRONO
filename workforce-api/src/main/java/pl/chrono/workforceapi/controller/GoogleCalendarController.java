package pl.chrono.workforceapi.controller;

import java.net.URI;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pl.chrono.timemanager.api.I_GoogleCalendarService;
import pl.chrono.timemanager.dto.response.GoogleCalendarStatusDto;
import pl.chrono.timemanager.exception.GoogleCalendarException;
import pl.chrono.usermanager.service.AuthService;

@Slf4j
@RestController
public class GoogleCalendarController extends BaseController {

  private final I_GoogleCalendarService googleCalendarService;

  @Value("${chrono.google.frontend-redirect-url:http://localhost:3000/ustawienia}")
  private String frontendRedirectUrl;

  public GoogleCalendarController(
      AuthService authService, I_GoogleCalendarService googleCalendarService) {
    super(authService);
    this.googleCalendarService = googleCalendarService;
  }

  @GetMapping("/api/google/calendar/connect")
  @PreAuthorize("isAuthenticated()")
  public ResponseEntity<Map<String, String>> connect(
      @RequestHeader("Authorization") String token) {
    Integer currentUserId = extractId(token);
    String url = googleCalendarService.getAuthorizationUrl(currentUserId);
    return ResponseEntity.ok(Map.of("url", url));
  }

  @GetMapping("/api/google/calendar/status")
  @PreAuthorize("isAuthenticated()")
  public ResponseEntity<GoogleCalendarStatusDto> getStatus(
      @RequestHeader("Authorization") String token) {
    Integer currentUserId = extractId(token);
    return ResponseEntity.ok(googleCalendarService.getStatus(currentUserId));
  }

  @GetMapping("/api/auth/google/callback")
  public ResponseEntity<Void> callback(
      @RequestParam(required = false) String code,
      @RequestParam(required = false) String state,
      @RequestParam(required = false) String error) {
    if (error != null && !error.isBlank()) {
      return redirect("?google=denied");
    }

    try {
      googleCalendarService.handleCallback(code, state);
      return redirect("?google=connected");
    } catch (GoogleCalendarException e) {
      log.warn("Google Calendar OAuth callback failed: {}", e.getMessage());
      return redirect("?google=error");
    }
  }

  private ResponseEntity<Void> redirect(String query) {
    return ResponseEntity.status(HttpStatus.FOUND)
        .location(URI.create(frontendRedirectUrl + query))
        .build();
  }
}
