package pl.chrono.reportmanager.dto;

import java.time.LocalDate;
import java.util.List;

public record TeamMatrixDay(
    LocalDate date,
    List<Long> workSeconds
) {}
