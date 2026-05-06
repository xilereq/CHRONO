package pl.chrono.usermanager.dto.request;

public record RegisterRequest(
   String email,
   String roleName
) {}
