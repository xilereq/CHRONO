package pl.chrono.reportmanager.service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.chrono.dbmanager.model.ScheduledReport;
import pl.chrono.dbmanager.model.User;
import pl.chrono.dbmanager.model.enums.ReportPeriod;
import pl.chrono.dbmanager.model.enums.ScheduleFrequency;
import pl.chrono.dbmanager.model.enums.ScheduleRunStatus;
import pl.chrono.dbmanager.model.enums.ScheduledReportType;
import pl.chrono.dbmanager.repository.ScheduledReportRepository;
import pl.chrono.dbmanager.repository.UserRepository;
import pl.chrono.reportmanager.api.I_ReportFacade;
import pl.chrono.reportmanager.dto.ReportFormat;
import pl.chrono.reportmanager.dto.ScheduledReportRequestDto;
import pl.chrono.reportmanager.dto.ScheduledReportResponseDto;
import pl.chrono.reportmanager.event.ReportAttachmentDto;
import pl.chrono.reportmanager.event.ScheduledReportEmailEvent;
import pl.chrono.reportmanager.event.ScheduledReportFailureEvent;

@Slf4j
@Service
@RequiredArgsConstructor
public class ScheduledReportService {

  private static final int MAX_RETRY_ATTEMPTS = 5;

  private final ScheduledReportRepository scheduledReportRepository;
  private final UserRepository userRepository;
  private final I_ReportFacade reportFacade;
  private final ApplicationEventPublisher eventPublisher;

  @Transactional
  public ScheduledReportResponseDto createSchedule(User currentUser, ScheduledReportRequestDto dto) {
    List<User> targetUsers = fetchAndValidatePermissions(currentUser, dto);

    ScheduledReport entity = new ScheduledReport();
    entity.setCreator(currentUser);
    applyDtoToEntity(entity, dto, targetUsers);
    entity.setActive(true);
    entity.setRetryCount(0);
    entity.setNextRunAt(calculateNextRunAt(entity, LocalDateTime.now()));

    ScheduledReport saved = scheduledReportRepository.save(entity);
    return mapToResponse(saved);
  }

  @Transactional
  public ScheduledReportResponseDto updateSchedule(User currentUser, Long scheduleId, ScheduledReportRequestDto dto) {
    ScheduledReport entity = getAccessibleSchedule(currentUser, scheduleId);
    List<User> targetUsers = fetchAndValidatePermissions(currentUser, dto);

    applyDtoToEntity(entity, dto, targetUsers);
    entity.setRetryCount(0);
    entity.setNextRunAt(calculateNextRunAt(entity, LocalDateTime.now()));

    return mapToResponse(scheduledReportRepository.save(entity));
  }

  @Transactional(readOnly = true)
  public List<ScheduledReportResponseDto> getSchedules(User currentUser) {
    List<ScheduledReport> schedules = isHrOrAdmin(currentUser)
        ? scheduledReportRepository.findAllWithDetails()
        : scheduledReportRepository.findAllByCreatorIdWithDetails(currentUser.getUserId());

    return schedules.stream().map(this::mapToResponse).toList();
  }

  @Transactional(readOnly = true)
  public ScheduledReportResponseDto getScheduleById(User currentUser, Long scheduleId) {
    return mapToResponse(getAccessibleSchedule(currentUser, scheduleId));
  }

  @Transactional
  public ScheduledReportResponseDto toggleSchedule(User currentUser, Long scheduleId) {
    ScheduledReport entity = getAccessibleSchedule(currentUser, scheduleId);
    boolean newState = !entity.isActive();
    entity.setActive(newState);

    if (newState) {
      entity.setRetryCount(0);
      entity.setNextRunAt(calculateNextRunAt(entity, LocalDateTime.now()));
    }
    return mapToResponse(scheduledReportRepository.save(entity));
  }

  @Transactional
  public void deleteSchedule(User currentUser, Long scheduleId) {
    ScheduledReport entity = getAccessibleSchedule(currentUser, scheduleId);
    scheduledReportRepository.delete(entity);
  }

  @Transactional
  public void triggerScheduleNow(User currentUser, Long scheduleId) {
    ScheduledReport schedule = getAccessibleSchedule(currentUser, scheduleId);
    generateAndSendReport(schedule);
    schedule.setLastRunAt(LocalDateTime.now());
    schedule.setLastRunStatus(ScheduleRunStatus.SUCCESS);
    scheduledReportRepository.save(schedule);
  }

  public void processDueSchedules() {
    LocalDateTime now = LocalDateTime.now();
    List<ScheduledReport> dueSchedules = scheduledReportRepository.findAllDueWithDetails(now);

    if (dueSchedules.isEmpty()) {
      return;
    }

    log.info("Znaleziono {} zaplanowanych raportów do wygenerowania.", dueSchedules.size());
    for (ScheduledReport schedule : dueSchedules) {
      processSingleScheduleExecution(schedule, now);
    }
  }

  @Transactional
  public void processSingleScheduleExecution(ScheduledReport schedule, LocalDateTime referenceTime) {
    try {
      generateAndSendReport(schedule);

      schedule.setLastRunAt(LocalDateTime.now());
      schedule.setLastRunStatus(ScheduleRunStatus.SUCCESS);
      schedule.setRetryCount(0);
      schedule.setNextRunAt(calculateNextRunAt(schedule, referenceTime));
      scheduledReportRepository.save(schedule);

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
      scheduledReportRepository.save(schedule);
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

      String safeUserName = (targetUser.getName() + "_" + targetUser.getSurname()).replaceAll("\\s+", "_");
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

    eventPublisher.publishEvent(new ScheduledReportEmailEvent(recipients, subject, body, attachments));
  }

  private byte[] invokeFacade(ScheduledReportType type, Integer userId,
                              LocalDate startDate, LocalDate endDate, ReportFormat format) {
    return switch (type) {
      case EMPLOYEE_TIMESHEET -> reportFacade.getEmployeeTimesheet(userId, startDate, endDate, format);
      case EMPLOYEE_OVERTIME -> reportFacade.getEmployeeOvertime(userId, startDate, endDate, format);
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
        LocalDate prevMonday = today.minusWeeks(1).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
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

  public LocalDateTime calculateNextRunAt(ScheduledReport schedule, LocalDateTime fromTime) {
    LocalTime execTime = schedule.getExecutionTime() != null ? schedule.getExecutionTime() : LocalTime.of(7, 0);
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
          candidate = nextMonth.withDayOfMonth(Math.min(dom, nextMonth.lengthOfMonth())).atTime(execTime);
        }
        yield candidate;
      }
    };
  }

  private List<User> fetchAndValidatePermissions(User currentUser, ScheduledReportRequestDto dto) {
    if (dto.frequency() == ScheduleFrequency.WEEKLY && dto.dayOfWeek() == null) {
      throw new IllegalArgumentException("Dla częstotliwości WEEKLY należy podać dzień tygodnia (1-7).");
    }
    if (dto.frequency() == ScheduleFrequency.MONTHLY && dto.dayOfMonth() == null) {
      throw new IllegalArgumentException("Dla częstotliwości MONTHLY należy podać dzień miesiąca (1-28).");
    }
    if (dto.reportPeriod() == ReportPeriod.CUSTOM) {
      if (dto.customStartDate() == null || dto.customEndDate() == null
          || dto.customStartDate().isAfter(dto.customEndDate())) {
        throw new IllegalArgumentException("Dla okresu CUSTOM wymagany jest poprawny zakres dat.");
      }
    }

    List<User> targetUsers = userRepository.findAllById(dto.targetUserIds());
    if (targetUsers.size() != new HashSet<>(dto.targetUserIds()).size()) {
      throw new IllegalArgumentException("Jeden lub więcej wskazanych użytkowników nie istnieje.");
    }

    boolean hrOrAdmin = isHrOrAdmin(currentUser);
    boolean manager = hasRole(currentUser, "ROLE_MANAGER");

    if (!hrOrAdmin && !manager) {
      if (dto.reportType().isTeamReport()) {
        throw new AccessDeniedException("Pracownik nie może planować raportów zespołowych.");
      }
      if (targetUsers.size() != 1 || !targetUsers.getFirst().getUserId().equals(currentUser.getUserId())) {
        throw new AccessDeniedException("Pracownik może zaplanować raport wyłącznie dla samego siebie.");
      }
      if (dto.recipientEmails().size() != 1 || !dto.recipientEmails().contains(currentUser.getEmail())) {
        throw new AccessDeniedException("Pracownik może wysyłać raporty wyłącznie na swój własny adres e-mail.");
      }
    } else if (manager && !hrOrAdmin) {
      boolean allAllowed = targetUsers.stream().allMatch(u ->
          u.getUserId().equals(currentUser.getUserId())
              || (u.getManager() != null && u.getManager().getUserId().equals(currentUser.getUserId()))
      );
      if (!allAllowed) {
        throw new AccessDeniedException("Manager może planować raporty wyłącznie dla siebie oraz swoich podwładnych.");
      }
    }

    return targetUsers;
  }

  private void applyDtoToEntity(ScheduledReport entity, ScheduledReportRequestDto dto, List<User> targetUsers) {
    entity.setName(dto.name());
    entity.setReportType(dto.reportType());
    entity.setReportFormat(dto.reportFormat().name());
    entity.setFrequency(dto.frequency());
    entity.setDayOfWeek(dto.dayOfWeek());
    entity.setDayOfMonth(dto.dayOfMonth());
    entity.setExecutionTime(dto.executionTime() != null ? dto.executionTime() : LocalTime.of(7, 0));
    entity.setReportPeriod(dto.reportPeriod());
    entity.setCustomStartDate(dto.customStartDate());
    entity.setCustomEndDate(dto.customEndDate());
    entity.setRecipientEmails(String.join(",", dto.recipientEmails()));
    entity.setTargetUsers(new HashSet<>(targetUsers));
  }

  private ScheduledReport getAccessibleSchedule(User currentUser, Long scheduleId) {
    ScheduledReport schedule = scheduledReportRepository.findByIdWithDetails(scheduleId)
        .orElseThrow(() -> new IllegalArgumentException("Nie znaleziono harmonogramu o ID: " + scheduleId));

    if (!isHrOrAdmin(currentUser) && !schedule.getCreator().getUserId().equals(currentUser.getUserId())) {
      throw new AccessDeniedException("Brak uprawnień do tego harmonogramu.");
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

  private ScheduledReportResponseDto mapToResponse(ScheduledReport entity) {
    return new ScheduledReportResponseDto(
        entity.getScheduleId(),
        entity.getName(),
        entity.getCreator().getUserId(),
        entity.getCreator().getName() + " " + entity.getCreator().getSurname(),
        entity.getReportType(),
        ReportFormat.valueOf(entity.getReportFormat()),
        entity.getFrequency(),
        entity.getDayOfWeek(),
        entity.getDayOfMonth(),
        entity.getExecutionTime(),
        entity.getReportPeriod(),
        entity.getCustomStartDate(),
        entity.getCustomEndDate(),
        entity.getTargetUsers().stream().map(User::getUserId).toList(),
        Arrays.asList(entity.getRecipientEmails().split(",")),
        entity.isActive(),
        entity.getNextRunAt(),
        entity.getLastRunAt(),
        entity.getLastRunStatus(),
        entity.getRetryCount()
    );
  }
}
