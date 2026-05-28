package pl.chrono.dbmanager.ports;

import pl.chrono.dbmanager.model.OvertimeArchive;
import pl.chrono.dbmanager.model.enums.OvertimeType;
import java.util.List;
import java.util.Optional;

public interface I_OvertimeData {

  Integer getUserOvertimeBalance(Integer userId);

  OvertimeArchive saveOvertimeRecord(OvertimeArchive record);

  List<OvertimeArchive> findUserOvertimeHistory(Integer userId);

  Optional<OvertimeArchive> findByApplicationAndType(Long applicationId, OvertimeType type);

}
