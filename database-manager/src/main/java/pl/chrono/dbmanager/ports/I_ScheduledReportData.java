package pl.chrono.dbmanager.ports;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import pl.chrono.dbmanager.model.ScheduledReport;

public interface I_ScheduledReportData {

  ScheduledReport saveScheduledReport(ScheduledReport scheduledReport);

  void deleteScheduledReport(ScheduledReport scheduledReport);

  Optional<ScheduledReport> findScheduledReportById(Long scheduleId);

  List<ScheduledReport> findAllScheduledReports();

  List<ScheduledReport> findAllScheduledReportsByCreatorId(Integer creatorId);

  List<ScheduledReport> findAllDueScheduledReports(LocalDateTime now);
}
