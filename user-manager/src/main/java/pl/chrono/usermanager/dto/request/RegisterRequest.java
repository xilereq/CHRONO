package pl.chrono.usermanager.dto.request;

public record RegisterRequest(
    String name,
    String surname,
    String email,
    String roleName
) {}
