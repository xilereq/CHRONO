package pl.chrono.usermanager.dto.request;

public record SetActive(
    String email,
    boolean active
) {}
