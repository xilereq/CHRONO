package pl.chrono.timemanager.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.chrono.dbmanager.model.Application;
import pl.chrono.dbmanager.model.OvertimeArchive;
import pl.chrono.dbmanager.model.WorkLog;
import pl.chrono.dbmanager.model.enums.OvertimeType;
import pl.chrono.dbmanager.ports.I_OvertimeData;
import pl.chrono.timemanager.dto.response.OvertimeRecordDto;
import pl.chrono.timemanager.exception.OvertimeException;
import pl.chrono.timemanager.mapper.OvertimeMapper;

@Service
@RequiredArgsConstructor
public class OvertimeService {
  private final I_OvertimeData overtimeData;
  private final OvertimeMapper overtimeMapper;

  // TODO: Aktualnie wnioski nie posiadają pola z czasem trwania. Na razie podstawiam 15 minut
  private final int duration = 15 * 60;

  @Transactional(readOnly = true)
  public Integer getOvertimeBalance(Integer userId) {
    return overtimeData.getUserOvertimeBalance(userId);
  }

  @Transactional
  public void reserveOvertimeForApplication(Application application) {
    // int duration = application.getDuration()

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
    // int duration = application.getDuration()

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
    // int duration = application.getDuration()

    if (duration <= 0) {
      return;
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
    // int duration = application.getDuration() - przykładowo

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

  private LocalDateTime calculateEndOfQuarter() {
    LocalDateTime now = LocalDateTime.now();
    int currentMonth = now.getMonthValue();
    int year = now.getYear();
    int quarterEndMonth = ((currentMonth - 1) / 3 + 1) * 3;
    LocalDate lastDayOfQuarter = LocalDate.of(year, quarterEndMonth, 1)
        .plusMonths(1).minusDays(1);
    return lastDayOfQuarter.atTime(23, 59, 59);
  }

  @Transactional(readOnly = true)
  public List<OvertimeRecordDto> getOvertimeHistory(Integer userId) {
    List<OvertimeArchive> historyEntities = overtimeData.findUserOvertimeHistory(userId);

    return historyEntities.stream()
        .map(overtimeMapper::toDto)
        .toList();
  }
}
