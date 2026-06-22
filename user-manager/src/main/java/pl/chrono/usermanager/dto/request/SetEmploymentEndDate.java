package pl.chrono.usermanager.dto.request;

import java.time.LocalDate;

public record SetEmploymentEndDate(
    String email,
    LocalDate date
) {}
