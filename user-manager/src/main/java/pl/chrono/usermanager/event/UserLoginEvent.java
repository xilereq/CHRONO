package pl.chrono.usermanager.event;

public record UserLoginEvent(
    Integer userId,
    String email
) {
}
