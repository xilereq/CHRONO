package pl.chrono.usermanager.dto.request;

public record ChangePasswordRequest (
    String newPassword,
    String oldPassword
) {}
