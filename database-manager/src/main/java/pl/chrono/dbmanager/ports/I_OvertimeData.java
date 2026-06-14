package pl.chrono.dbmanager.ports;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import pl.chrono.dbmanager.model.OvertimeArchive;
import pl.chrono.dbmanager.model.enums.OvertimeType;

public interface I_OvertimeData {

  Integer getUserOvertimeBalance(Integer userId);

  Integer sumEarnedOvertimeBetween(Integer userId, LocalDateTime startOfYear,
                                   LocalDateTime endOfYear);

  OvertimeArchive saveOvertimeRecord(OvertimeArchive record);

  List<OvertimeArchive> findUserOvertimeHistory(Integer userId);

  Optional<OvertimeArchive> findByApplicationAndType(Long applicationId, OvertimeType type);

  Integer getExpiringOvertimeBalance(Integer userId, LocalDateTime threshold);

  List<OvertimeArchive> findByWorkLogId(Integer workLogId);

  void deleteOvertimeRecord(OvertimeArchive overtimeArchive);

}
