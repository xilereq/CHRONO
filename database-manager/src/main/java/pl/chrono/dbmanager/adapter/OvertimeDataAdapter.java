package pl.chrono.dbmanager.adapter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import pl.chrono.dbmanager.model.OvertimeArchive;
import pl.chrono.dbmanager.model.enums.OvertimeType;
import pl.chrono.dbmanager.ports.I_OvertimeData;
import pl.chrono.dbmanager.repository.OvertimeArchiveRepository;

@Component
@RequiredArgsConstructor
public class OvertimeDataAdapter implements I_OvertimeData {
  private final OvertimeArchiveRepository repository;

  @Override
  public Integer getUserOvertimeBalance(Integer userId) {
    return repository.calculateOvertimeBalanceSeconds(userId);
  }

  @Override
  public Integer sumEarnedOvertimeBetween(Integer userId, LocalDateTime startOfYear, LocalDateTime endOfYear) {
    return repository.sumEarnedOvertimeBetween(userId, startOfYear, endOfYear);
  }

  @Override
  public OvertimeArchive saveOvertimeRecord(OvertimeArchive record) {
    return repository.save(record);
  }

  @Override
  public List<OvertimeArchive> findUserOvertimeHistory(Integer userId) {
    return repository.findByUser_UserId(userId);
  }

  @Override
  public Optional<OvertimeArchive> findByApplicationAndType(Long applicationId, OvertimeType type) {
    return repository.findByApplication_IdAndType(applicationId, type);
  }

  @Override
  public Integer getExpiringOvertimeBalance(Integer userId, LocalDateTime threshold) {
    return repository.calculateExpiringBalanceSeconds(userId, threshold);
  }

  @Override
  public List<OvertimeArchive> findByWorkLogId(Integer workLogId) {
    return repository.findByWorkLog_WorkLogId(workLogId);
  }

  @Override
  public void deleteOvertimeRecord(OvertimeArchive overtimeArchive) {
    repository.delete(overtimeArchive);
  }
}
