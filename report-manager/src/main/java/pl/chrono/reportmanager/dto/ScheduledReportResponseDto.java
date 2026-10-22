package pl.chrono.reportmanager.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import pl.chrono.dbmanager.model.enums.ReportPeriod;
import pl.chrono.dbmanager.model.enums.ScheduleFrequency;
import pl.chrono.dbmanager.model.enums.ScheduleRunStatus;
import pl.chrono.dbmanager.model.enums.ScheduledReportType;

public record ScheduledReportResponseDto(
    Long scheduleId,
    String name,
    Integer creatorId,
    String creatorFullName,
    ScheduledReportType reportType,
    ReportFormat reportFormat,
    ScheduleFrequency frequency,
    Integer dayOfWeek,
    Integer dayOfMonth,
    LocalTime executionTime,
    ReportPeriod reportPeriod,
    LocalDate customStartDate,
    LocalDate customEndDate,
    List<Integer> targetUserIds,
    List<String> recipientEmails,
    boolean isActive,
    LocalDateTime nextRunAt,
    LocalDateTime lastRunAt,
    ScheduleRunStatus lastRunStatus,
    int retryCount
) {}
