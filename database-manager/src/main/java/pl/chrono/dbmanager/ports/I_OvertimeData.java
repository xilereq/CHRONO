package pl.chrono.dbmanager.ports;

import pl.chrono.dbmanager.model.OvertimeArchive;
import java.util.List;

public interface I_OvertimeData {

  Integer getUserOvertimeBalance(Integer userId);

  OvertimeArchive saveOvertimeRecord(OvertimeArchive record);

  List<OvertimeArchive> findUserOvertimeHistory(Integer userId);

}
