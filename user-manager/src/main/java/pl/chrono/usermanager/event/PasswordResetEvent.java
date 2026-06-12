package pl.chrono.usermanager.event;

public record PasswordResetEvent(
    Integer userId,
    String email,
    String resetToken
) {
}
