package pl.chrono.usermanager.dto.request;

public record SetManagerRequest(
    String email,
    String managerEmail
) {}
