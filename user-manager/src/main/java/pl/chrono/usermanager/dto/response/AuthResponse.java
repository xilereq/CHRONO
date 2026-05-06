package pl.chrono.usermanager.dto.response;

public record AuthResponse (
  String token,
  String role,
  boolean forcePasswordChange
) {}
