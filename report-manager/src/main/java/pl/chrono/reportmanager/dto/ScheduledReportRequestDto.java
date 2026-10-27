package pl.chrono.reportmanager.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import pl.chrono.dbmanager.model.enums.ReportPeriod;
import pl.chrono.dbmanager.model.enums.ScheduleFrequency;
import pl.chrono.dbmanager.model.enums.ScheduledReportType;

public record ScheduledReportRequestDto(
    @NotBlank(message = "Nazwa harmonogramu jest wymagana")
    String name,

    @NotNull(message = "Typ raportu jest wymagany")
    ScheduledReportType reportType,

    @NotNull(message = "Format raportu jest wymagany")
    ReportFormat reportFormat,

    @NotNull(message = "Częstotliwość jest wymagana")
    ScheduleFrequency frequency,

    @Min(1) @Max(7)
    Integer dayOfWeek,

    @Min(1) @Max(28)
    Integer dayOfMonth,

    LocalTime executionTime,

    @NotNull(message = "Okres raportowania jest wymagany")
    ReportPeriod reportPeriod,

    LocalDate customStartDate,
    LocalDate customEndDate,

    @NotEmpty(message = "Lista użytkowników objętych raportem nie może być pusta")
    List<Integer> targetUserIds,

    @NotEmpty(message = "Lista adresów e-mail odbiorców nie może być pusta")
    List<String> recipientEmails
) {}
