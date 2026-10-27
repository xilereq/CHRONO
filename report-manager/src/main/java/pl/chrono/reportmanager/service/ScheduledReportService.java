package pl.chrono.reportmanager.service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.chrono.dbmanager.model.ScheduledReport;
import pl.chrono.dbmanager.model.User;
import pl.chrono.dbmanager.model.enums.ReportPeriod;
import pl.chrono.dbmanager.model.enums.ScheduleFrequency;
import pl.chrono.dbmanager.model.enums.ScheduleRunStatus;
import pl.chrono.dbmanager.model.enums.ScheduledReportType;
import pl.chrono.dbmanager.ports.I_ScheduledReportData;
import pl.chrono.dbmanager.ports.I_UserData;
import pl.chrono.logmanager.audit.LogActivity;
import pl.chrono.reportmanager.api.I_ReportFacade;
import pl.chrono.reportmanager.api.I_ScheduledReportService;
import pl.chrono.reportmanager.dto.ReportFormat;
import pl.chrono.reportmanager.dto.ScheduledReportRequestDto;
import pl.chrono.reportmanager.dto.ScheduledReportResponseDto;
import pl.chrono.reportmanager.event.ReportAttachmentDto;
import pl.chrono.reportmanager.event.ScheduledReportEmailEvent;
import pl.chrono.reportmanager.event.ScheduledReportFailureEvent;
import pl.chrono.reportmanager.exception.ReportScheduleException;
import pl.chrono.reportmanager.exception.ReportScheduleSecurityException;
import pl.chrono.reportmanager.exception.ReportUserException;
import pl.chrono.reportmanager.mapper.ScheduledReportMapper;

@Slf4j
@Service
@RequiredArgsConstructor
public class ScheduledReportService implements I_ScheduledReportService {

  private static final int MAX_RETRY_ATTEMPTS = 5;

  private final I_ScheduledReportData scheduledReportData;
  private final I_UserData userData;
  private final ScheduledReportMapper scheduledReportMapper;
  private final I_ReportFacade reportFacade;
  private final ApplicationEventPublisher eventPublisher;

  @Override
  @Transactional
  @LogActivity(action = "CREATE_SCHEDULED_REPORT", details = "Created scheduled report")
  public ScheduledReportResponseDto createSchedule(
      Integer currentUserId, ScheduledReportRequestDto dto) {
    User currentUser = getCurrentUser(currentUserId);
    List<User> targetUsers = fetchAndValidatePermissions(currentUser, dto);

    ScheduledReport entity = new ScheduledReport();
    entity.setCreator(currentUser);
    scheduledReportMapper.updateEntityFromDto(dto, entity);
    entity.setTargetUsers(new HashSet<>(targetUsers));
    entity.setActive(true);
    entity.setRetryCount(0);
    entity.setNextRunAt(calculateNextRunAt(entity, LocalDateTime.now()));

    ScheduledReport saved = scheduledReportData.saveScheduledReport(entity);
    return scheduledReportMapper.toDto(saved);
  }

  @Override
  @Transactional
  @LogActivity(action = "UPDATE_SCHEDULED_REPORT", details = "Updated scheduled report")
  public ScheduledReportResponseDto updateSchedule(
      Integer currentUserId, Long scheduleId, ScheduledReportRequestDto dto) {
    User currentUser = getCurrentUser(currentUserId);
    ScheduledReport entity = getAccessibleSchedule(currentUser, scheduleId);
    List<User> targetUsers = fetchAndValidatePermissions(currentUser, dto);

    scheduledReportMapper.updateEntityFromDto(dto, entity);
    entity.setTargetUsers(new HashSet<>(targetUsers));
    entity.setRetryCount(0);
    entity.setNextRunAt(calculateNextRunAt(entity, LocalDateTime.now()));

    return scheduledReportMapper.toDto(scheduledReportData.saveScheduledReport(entity));
  }

  @Override
  @Transactional(readOnly = true)
  public List<ScheduledReportResponseDto> getSchedules(Integer currentUserId) {
    User currentUser = getCurrentUser(currentUserId);
    List<ScheduledReport> schedules = isHrOrAdmin(currentUser)
        ? scheduledReportData.findAllScheduledReports()
        : scheduledReportData.findAllScheduledReportsByCreatorId(currentUser.getUserId());

    return schedules.stream().map(scheduledReportMapper::toDto).toList();
  }

  @Override
  @Transactional(readOnly = true)
  public ScheduledReportResponseDto getScheduleById(Integer currentUserId, Long scheduleId) {
    User currentUser = getCurrentUser(currentUserId);
    return scheduledReportMapper.toDto(getAccessibleSchedule(currentUser, scheduleId));
  }

  @Override
  @Transactional
  @LogActivity(action = "TOGGLE_SCHEDULED_REPORT", details = "Toggled scheduled report status")
  public ScheduledReportResponseDto toggleSchedule(Integer currentUserId, Long scheduleId) {
    User currentUser = getCurrentUser(currentUserId);
    ScheduledReport entity = getAccessibleSchedule(currentUser, scheduleId);
    boolean newState = !entity.isActive();
    entity.setActive(newState);

    if (newState) {
      entity.setRetryCount(0);
      entity.setNextRunAt(calculateNextRunAt(entity, LocalDateTime.now()));
    }
    return scheduledReportMapper.toDto(scheduledReportData.saveScheduledReport(entity));
  }

  @Override
  @Transactional
  @LogActivity(action = "DELETE_SCHEDULED_REPORT", details = "Deleted scheduled report")
  public void deleteSchedule(Integer currentUserId, Long scheduleId) {
    User currentUser = getCurrentUser(currentUserId);
    ScheduledReport entity = getAccessibleSchedule(currentUser, scheduleId);
    scheduledReportData.deleteScheduledReport(entity);
  }

  @Override
  @Transactional
  @LogActivity(action = "TRIGGER_SCHEDULED_REPORT", details = "Triggered scheduled report manually")
  public void triggerScheduleNow(Integer currentUserId, Long scheduleId) {
    User currentUser = getCurrentUser(currentUserId);
    ScheduledReport schedule = getAccessibleSchedule(currentUser, scheduleId);
    generateAndSendReport(schedule);
    schedule.setLastRunAt(LocalDateTime.now());
    schedule.setLastRunStatus(ScheduleRunStatus.SUCCESS);
    scheduledReportData.saveScheduledReport(schedule);
  }

  @Override
  public void processDueSchedules() {
    LocalDateTime now = LocalDateTime.now();
    List<ScheduledReport> dueSchedules = scheduledReportData.findAllDueScheduledReports(now);

    if (dueSchedules.isEmpty()) {
      return;
    }

    log.info("Znaleziono {} zaplanowanych raportów do wygenerowania.", dueSchedules.size());
    for (ScheduledReport schedule : dueSchedules) {
      processSingleScheduleExecution(schedule, now);
    }
  }

  void processSingleScheduleExecution(ScheduledReport schedule, LocalDateTime referenceTime) {
    try {
      generateAndSendReport(schedule);

      schedule.setLastRunAt(LocalDateTime.now());
      schedule.setLastRunStatus(ScheduleRunStatus.SUCCESS);
      schedule.setRetryCount(0);
      schedule.setNextRunAt(calculateNextRunAt(schedule, referenceTime));
      scheduledReportData.saveScheduledReport(schedule);

    } catch (Exception e) {
      int attempts = schedule.getRetryCount() + 1;
      schedule.setRetryCount(attempts);
      schedule.setLastRunAt(LocalDateTime.now());
      schedule.setLastRunStatus(ScheduleRunStatus.FAILED);

      log.error("Błąd generowania raportu cyklicznego ID={} (próba {}/{}): {}",
          schedule.getScheduleId(), attempts, MAX_RETRY_ATTEMPTS, e.getMessage());

      if (attempts >= MAX_RETRY_ATTEMPTS) {
        String errorMsg = String.format(
            "Nie udało się wygenerować raportu cyklicznego '%s' po %d próbach. Harmonogram został przesunięty na kolejny cykl.",
            schedule.getName(), MAX_RETRY_ATTEMPTS);

        eventPublisher.publishEvent(new ScheduledReportFailureEvent(
            schedule.getCreator().getUserId(),
            schedule.getScheduleId(),
            errorMsg
        ));

        schedule.setRetryCount(0);
        schedule.setNextRunAt(calculateNextRunAt(schedule, referenceTime));
      }
      scheduledReportData.saveScheduledReport(schedule);
    }
  }

  private void generateAndSendReport(ScheduledReport schedule) {
    LocalDate[] dateRange = resolveDateRange(schedule);
    LocalDate startDate = dateRange[0];
    LocalDate endDate = dateRange[1];

    ReportFormat format = ReportFormat.valueOf(schedule.getReportFormat());
    String extension = resolveFileExtension(format);
    String contentType = resolveContentType(format);

    List<ReportAttachmentDto> attachments = new ArrayList<>();

    for (User targetUser : schedule.getTargetUsers()) {
      byte[] reportBytes = invokeFacade(
          schedule.getReportType(),
          targetUser.getUserId(),
          startDate,
          endDate,
          format
      );

      String safeUserName =
          (targetUser.getName() + "_" + targetUser.getSurname()).replaceAll("\\s+", "_");
      String fileName = String.format("%s_%s_%s_%s.%s",
          schedule.getReportType().name(),
          safeUserName,
          startDate,
          endDate,
          extension
      );

      attachments.add(new ReportAttachmentDto(fileName, reportBytes, contentType));
    }

    String[] recipients = Arrays.stream(schedule.getRecipientEmails().split(","))
        .map(String::trim)
        .filter(s -> !s.isEmpty())
        .toArray(String[]::new);

    String subject = String.format("[Chrono] Raport cykliczny: %s (%s - %s)",
        schedule.getName(), startDate, endDate);

    String body = String.format(
        """
            Cześć,
            
            W załączniku przesyłamy automatycznie wygenerowany raport '%s'.
            Typ raportu: %s
            Format: %s (.%s)
            Zakres dat: %s do %s
            
            Pozdrawiamy,
            Zespół Chrono Workforce""",
        schedule.getName(),
        schedule.getReportType().name(),
        format.name(),
        extension,
        startDate,
        endDate
    );

    eventPublisher.publishEvent(
        new ScheduledReportEmailEvent(recipients, subject, body, attachments));
  }

  private byte[] invokeFacade(ScheduledReportType type, Integer userId,
                              LocalDate startDate, LocalDate endDate, ReportFormat format) {
    return switch (type) {
      case EMPLOYEE_TIMESHEET ->
          reportFacade.getEmployeeTimesheet(userId, startDate, endDate, format);
      case EMPLOYEE_OVERTIME ->
          reportFacade.getEmployeeOvertime(userId, startDate, endDate, format);
      case TEAM_WORKLOAD -> reportFacade.getTeamWorkload(userId, startDate, endDate, format);
      case TEAM_ABSENCES -> reportFacade.getTeamAbsences(userId, startDate, endDate, format);
      case TEAM_OVERTIME -> reportFacade.getTeamOvertime(userId, startDate, endDate, format);
      case TEAM_ANOMALY -> reportFacade.getTeamAnomalyReport(userId, startDate, endDate, format);
    };
  }

  private LocalDate[] resolveDateRange(ScheduledReport schedule) {
    LocalDate today = LocalDate.now();
    return switch (schedule.getReportPeriod()) {
      case PREVIOUS_DAY -> new LocalDate[]{today.minusDays(1), today.minusDays(1)};
      case CURRENT_WEEK -> new LocalDate[]{
          today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)),
          today
      };
      case PREVIOUS_WEEK -> {
        LocalDate prevMonday =
            today.minusWeeks(1).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        yield new LocalDate[]{prevMonday, prevMonday.plusDays(6)};
      }
      case CURRENT_MONTH -> new LocalDate[]{today.withDayOfMonth(1), today};
      case PREVIOUS_MONTH -> {
        LocalDate prevMonth = today.minusMonths(1);
        yield new LocalDate[]{
            prevMonth.withDayOfMonth(1),
            prevMonth.with(TemporalAdjusters.lastDayOfMonth())
        };
      }
      case LAST_3_MONTHS -> new LocalDate[]{today.minusMonths(3), today};
      case LAST_6_MONTHS -> new LocalDate[]{today.minusMonths(6), today};
      case LAST_YEAR -> new LocalDate[]{today.minusYears(1), today};
      case CUSTOM -> new LocalDate[]{schedule.getCustomStartDate(), schedule.getCustomEndDate()};
    };
  }

  LocalDateTime calculateNextRunAt(ScheduledReport schedule, LocalDateTime fromTime) {
    LocalTime execTime = schedule.getExecutionTime() != null
        ? schedule.getExecutionTime()
        : LocalTime.of(7, 0);
    LocalDate baseDate = fromTime.toLocalDate();

    return switch (schedule.getFrequency()) {
      case DAILY -> {
        LocalDateTime candidate = baseDate.atTime(execTime);
        yield candidate.isAfter(fromTime) ? candidate : candidate.plusDays(1);
      }
      case WEEKLY -> {
        int dow = schedule.getDayOfWeek() != null ? schedule.getDayOfWeek() : 1;
        LocalDate nextDate = baseDate.with(TemporalAdjusters.nextOrSame(DayOfWeek.of(dow)));
        LocalDateTime candidate = nextDate.atTime(execTime);
        yield candidate.isAfter(fromTime) ? candidate : candidate.plusWeeks(1);
      }
      case MONTHLY -> {
        int dom = schedule.getDayOfMonth() != null ? Math.min(schedule.getDayOfMonth(), 28) : 1;
        LocalDate candidateDate = baseDate.withDayOfMonth(Math.min(dom, baseDate.lengthOfMonth()));
        LocalDateTime candidate = candidateDate.atTime(execTime);
        if (!candidate.isAfter(fromTime)) {
          LocalDate nextMonth = baseDate.plusMonths(1);
          candidate = nextMonth
              .withDayOfMonth(Math.min(dom, nextMonth.lengthOfMonth()))
              .atTime(execTime);
        }
        yield candidate;
      }
    };
  }

  private User getCurrentUser(Integer currentUserId) {
    if (currentUserId == null) {
      throw new ReportUserException.NotFound(null);
    }
    return userData.findById(currentUserId)
        .orElseThrow(() -> new ReportUserException.NotFound(currentUserId));
  }

  private List<User> fetchAndValidatePermissions(User currentUser, ScheduledReportRequestDto dto) {
    if (dto.frequency() == ScheduleFrequency.WEEKLY && dto.dayOfWeek() == null) {
      throw new ReportScheduleException.MissingDayOfWeek();
    }
    if (dto.frequency() == ScheduleFrequency.MONTHLY && dto.dayOfMonth() == null) {
      throw new ReportScheduleException.MissingDayOfMonth();
    }
    if (dto.reportPeriod() == ReportPeriod.CUSTOM
        && (dto.customStartDate() == null
        || dto.customEndDate() == null
        || dto.customStartDate().isAfter(dto.customEndDate()))) {
      throw new ReportScheduleException.InvalidCustomDateRange();
    }

    List<User> targetUsers = userData.findAllByIds(dto.targetUserIds());
    Set<Integer> requestedIds = new LinkedHashSet<>(dto.targetUserIds());
    if (targetUsers.size() != requestedIds.size()) {
      Set<Integer> foundIds = targetUsers.stream()
          .map(User::getUserId)
          .collect(Collectors.toSet());
      List<Integer> missingIds = requestedIds.stream()
          .filter(id -> !foundIds.contains(id))
          .toList();
      throw new ReportScheduleException.UsersNotFound(missingIds);
    }

    boolean hrOrAdmin = isHrOrAdmin(currentUser);
    boolean manager = hasRole(currentUser, "ROLE_MANAGER");

    if (!hrOrAdmin && !manager) {
      if (dto.reportType().isTeamReport()) {
        throw new ReportScheduleSecurityException.UnauthorizedTeamReport();
      }
      if (targetUsers.size() != 1
          || !targetUsers.getFirst().getUserId().equals(currentUser.getUserId())) {
        throw new ReportScheduleSecurityException.UnauthorizedTargetUser();
      }
      if (dto.recipientEmails().size() != 1
          || !dto.recipientEmails().contains(currentUser.getEmail())) {
        throw new ReportScheduleSecurityException.UnauthorizedRecipientEmail();
      }
    } else if (manager && !hrOrAdmin) {
      boolean allAllowed = targetUsers.stream().allMatch(u ->
          u.getUserId().equals(currentUser.getUserId())
              || (u.getManager() != null
              && u.getManager().getUserId().equals(currentUser.getUserId()))
      );
      if (!allAllowed) {
        throw new ReportScheduleSecurityException.UnauthorizedManagerTargetUsers();
      }
    }

    return targetUsers;
  }

  private ScheduledReport getAccessibleSchedule(User currentUser, Long scheduleId) {
    ScheduledReport schedule = scheduledReportData.findScheduledReportById(scheduleId)
        .orElseThrow(() -> new ReportScheduleException.NotFound(scheduleId));

    if (!isHrOrAdmin(currentUser)
        && !schedule.getCreator().getUserId().equals(currentUser.getUserId())) {
      throw new ReportScheduleSecurityException.UnauthorizedScheduleAccess();
    }
    return schedule;
  }

  private boolean isHrOrAdmin(User user) {
    return hasRole(user, "ROLE_HR") || hasRole(user, "ROLE_ADMIN");
  }

  private boolean hasRole(User user, String roleName) {
    return user.getAuthorities().stream()
        .anyMatch(a -> Objects.equals(a.getAuthority(), roleName));
  }

  private String resolveFileExtension(ReportFormat format) {
    return switch (format) {
      case PDF -> "pdf";
      case XLSX -> "xlsx";
      case CSV -> "csv";
    };
  }

  private String resolveContentType(ReportFormat format) {
    return switch (format) {
      case PDF -> "application/pdf";
      case XLSX -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
      case CSV -> "text/csv";
    };
  }
}
