package pl.chrono.usermanager.dto.request;

public record PasswordResetConfirmRequest(
    String token,
    String password
) {}
