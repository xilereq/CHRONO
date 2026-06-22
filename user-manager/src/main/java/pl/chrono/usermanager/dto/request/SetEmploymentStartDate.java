package pl.chrono.usermanager.dto.request;

import java.time.LocalDate;

public record SetEmploymentStartDate(
    String email,
    LocalDate date
) {}
