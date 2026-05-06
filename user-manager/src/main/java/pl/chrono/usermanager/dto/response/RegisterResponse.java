package pl.chrono.usermanager.dto.response;

public record RegisterResponse(
    String email,
    String temporaryPassword
) {}
