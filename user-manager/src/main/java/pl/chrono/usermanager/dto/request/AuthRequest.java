package pl.chrono.usermanager.dto.request;

public record AuthRequest (
    String email,
    String password
) {}
