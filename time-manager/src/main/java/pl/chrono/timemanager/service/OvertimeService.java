package pl.chrono.timemanager.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.chrono.dbmanager.model.Application;
import pl.chrono.dbmanager.model.OvertimeArchive;
import pl.chrono.dbmanager.model.User;
import pl.chrono.dbmanager.model.WorkLog;
import pl.chrono.dbmanager.model.enums.OvertimeType;
import pl.chrono.dbmanager.ports.I_OvertimeData;
import pl.chrono.dbmanager.repository.UserRepository;
import pl.chrono.timemanager.dto.response.OvertimeRecordDto;
import pl.chrono.timemanager.event.LawViolationEvent;
import pl.chrono.timemanager.exception.OvertimeException;
import pl.chrono.timemanager.mapper.OvertimeMapper;

@Service
@RequiredArgsConstructor
public class OvertimeService {
  private final I_OvertimeData overtimeData;
  private final OvertimeMapper overtimeMapper;
  private final UserRepository userRepository;
  private final ApplicationEventPublisher eventPublisher;

  @Transactional(readOnly = true)
  public Integer getOvertimeBalance(Integer userId) {
    return overtimeData.getUserOvertimeBalance(userId);
  }

  @Transactional
  public void reserveOvertimeForApplication(Application application) {
    int duration = application.getDurationSeconds() != null ? application.getDurationSeconds() : 0;

    int currentBalance = overtimeData.getUserOvertimeBalance(application.getEmployee().getUserId());
    if (currentBalance < duration) {
      throw new OvertimeException.InsufficientBalance(currentBalance);
    }
    OvertimeArchive archive = new OvertimeArchive();
    archive.setUser(application.getEmployee());
    archive.setType(OvertimeType.LOCKED);
    archive.setDurationSeconds(duration);
    archive.setApplication(application);
    archive.setValidUntil(calculateEndOfQuarter());
    archive.setNote("Reservation for application ID: " + application.getId());
    overtimeData.saveOvertimeRecord(archive);
  }

  @Transactional
  public void refundOvertimeForApplication(Application application) {
    int duration = application.getDurationSeconds() != null ? application.getDurationSeconds() : 0;

    OvertimeArchive archive = new OvertimeArchive();
    archive.setUser(application.getEmployee());
    archive.setType(OvertimeType.REFUNDED);
    archive.setDurationSeconds(duration);
    archive.setApplication(application);
    archive.setValidUntil(calculateEndOfQuarter());
    archive.setNote("Refunded for application ID: " + application.getId());
    overtimeData.saveOvertimeRecord(archive);
  }

  @Transactional
  public void grantOvertime(Application application, WorkLog workLog) {
    int duration = application.getDurationSeconds() != null ? application.getDurationSeconds() : 0;
    if (duration <= 0) {
      return;
    }

    int currentYear = LocalDate.now().getYear();
    LocalDateTime startOfYear = LocalDateTime.of(currentYear, 1, 1, 0, 0);
    LocalDateTime endOfYear = LocalDateTime.of(currentYear, 12, 31, 23, 59, 59);

    long yearlyOvertime = overtimeData.sumEarnedOvertimeBetween(
        application.getEmployee().getUserId(), startOfYear, endOfYear);
    int yearlyLimit = 150 * 3600;
    if (yearlyOvertime + duration > yearlyLimit && yearlyOvertime < yearlyLimit) {
      eventPublisher.publishEvent(new LawViolationEvent(
          application.getEmployee().getUserId(), "VIOLATION_YEARLY_OVERTIME", application.getId(),
          "APPLICATION",
          "UWAGA: Przekraczasz roczny limit 150 nadgodzin w roku " + currentYear + "."
      ));
    }

    OvertimeArchive archive = new OvertimeArchive();
    archive.setUser(workLog.getUser());
    archive.setType(OvertimeType.EARNED);
    archive.setDurationSeconds(duration);
    archive.setApplication(application);
    archive.setWorkLog(workLog);
    archive.setValidUntil(calculateEndOfQuarter());
    archive.setNote("Awarded for day: " + workLog.getStartTime().toLocalDate());
    overtimeData.saveOvertimeRecord(archive);
  }

  @Transactional
  public void registerDebt(Application application, WorkLog workLog) {
    int duration = application.getDurationSeconds() != null ? application.getDurationSeconds() : 0;

    if (duration <= 0) {
      return;
    }
    OvertimeArchive archive = new OvertimeArchive();
    archive.setUser(application.getEmployee());
    archive.setType(OvertimeType.DEBT);
    archive.setDurationSeconds(duration);
    archive.setApplication(application);
    archive.setWorkLog(workLog);
    archive.setValidUntil(calculateEndOfQuarter());
    archive.setNote("Debt for day: " + workLog.getStartTime().toLocalDate());
    overtimeData.saveOvertimeRecord(archive);
  }

  @Transactional
  public void changeOvertimeStatus(Application application, WorkLog workLog) {
    overtimeData.findByApplicationAndType(application.getId(), OvertimeType.LOCKED)
        .ifPresent(archive -> {
          archive.setType(OvertimeType.USED);
          archive.setWorkLog(workLog);
          archive.setNote("Used for work log on: " + workLog.getStartTime().toLocalDate());
          overtimeData.saveOvertimeRecord(archive);
        });
  }

  private LocalDateTime calculateEndOfQuarter() {
    LocalDateTime now = LocalDateTime.now();
    int currentMonth = now.getMonthValue();
    int year = now.getYear();
    int quarterEndMonth = ((currentMonth - 1) / 3 + 1) * 3;
    LocalDate lastDayOfQuarter = LocalDate.of(year, quarterEndMonth, 1).plusMonths(1).minusDays(1);
    return lastDayOfQuarter.atTime(23, 59, 59);
  }

  @Transactional
  public void expireUnusedOvertime(Integer userId) {
    LocalDateTime now = LocalDateTime.now();
    Integer expiredBalance = overtimeData.getExpiringOvertimeBalance(userId, now);

    if (expiredBalance == null || expiredBalance <= 0) {
      return;
    }

    OvertimeArchive archive = new OvertimeArchive();
    archive.setUser(userRepository.getReferenceById(userId));
    archive.setType(OvertimeType.EXPIRED);
    archive.setDurationSeconds(expiredBalance);
    archive.setValidUntil(now);
    archive.setNote(
        "Automatically expired unused overtime for quarter: " +
            (((now.getMonthValue() - 1) / 3) + 1) + " of year " + now.getYear());
    overtimeData.saveOvertimeRecord(archive);
  }

  @Transactional(readOnly = true)
  public List<OvertimeRecordDto> getOvertimeHistory(Integer userId) {
    List<OvertimeArchive> historyEntities = overtimeData.findUserOvertimeHistory(userId);

    return historyEntities.stream().map(overtimeMapper::toDto).toList();
  }

  @Transactional
  public void revertSettlementForWorkLog(Integer workLogId) {
    List<OvertimeArchive> archives = overtimeData.findByWorkLogId(workLogId);

    for (OvertimeArchive archive : archives) {
      if (archive.getType() == OvertimeType.EARNED || archive.getType() == OvertimeType.DEBT) {
        overtimeData.deleteOvertimeRecord(archive);
      } else if (archive.getType() == OvertimeType.USED) {
        archive.setType(OvertimeType.LOCKED);
        archive.setWorkLog(null);
        archive.setNote("Reverted to LOCKED due to work log modification.");
        overtimeData.saveOvertimeRecord(archive);
      }
    }
  }

  @Transactional
  public void registerPenaltyDebt(WorkLog workLog, int missingSeconds) {
    if (missingSeconds <= 0) {
      return;
    }
    OvertimeArchive archive = new OvertimeArchive();
    archive.setUser(workLog.getUser());
    archive.setType(OvertimeType.DEBT);
    archive.setDurationSeconds(missingSeconds);
    archive.setApplication(null);
    archive.setWorkLog(workLog);
    archive.setValidUntil(calculateEndOfQuarter());
    archive.setNote("Automatic penalty for incomplete workday without an application on: "
        + workLog.getStartTime().toLocalDate());
    overtimeData.saveOvertimeRecord(archive);
  }

  @Transactional
  public void registerPenaltyDebt(User user, LocalDate date, int missingSeconds) {
    if (missingSeconds <= 0) {
      return;
    }
    OvertimeArchive archive = new OvertimeArchive();
    archive.setUser(user);
    archive.setType(OvertimeType.DEBT);
    archive.setDurationSeconds(missingSeconds);
    archive.setApplication(null);
    archive.setWorkLog(null);
    archive.setValidUntil(calculateEndOfQuarter());
    archive.setNote("Automatic penalty for abandoned work log on: " + date);
    overtimeData.saveOvertimeRecord(archive);
  }
}
