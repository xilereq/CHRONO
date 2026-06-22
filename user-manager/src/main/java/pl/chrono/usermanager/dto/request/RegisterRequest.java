package pl.chrono.usermanager.dto.request;

import java.time.LocalDate;

public record RegisterRequest(
    String name,
    String surname,
    String email,
    String roleName,
    String managerEmail,
    LocalDate date
) {}
