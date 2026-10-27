package pl.chrono.dbmanager.adapter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import pl.chrono.dbmanager.model.ScheduledReport;
import pl.chrono.dbmanager.ports.I_ScheduledReportData;
import pl.chrono.dbmanager.repository.ScheduledReportRepository;

@Component
@RequiredArgsConstructor
public class ScheduledReportDataAdapter implements I_ScheduledReportData {

  private final ScheduledReportRepository scheduledReportRepository;

  @Override
  public ScheduledReport saveScheduledReport(ScheduledReport scheduledReport) {
    return scheduledReportRepository.save(scheduledReport);
  }

  @Override
  public void deleteScheduledReport(ScheduledReport scheduledReport) {
    scheduledReportRepository.delete(scheduledReport);
  }

  @Override
  public Optional<ScheduledReport> findScheduledReportById(Long scheduleId) {
    return scheduledReportRepository.findByIdWithDetails(scheduleId);
  }

  @Override
  public List<ScheduledReport> findAllScheduledReports() {
    return scheduledReportRepository.findAllWithDetails();
  }

  @Override
  public List<ScheduledReport> findAllScheduledReportsByCreatorId(Integer creatorId) {
    return scheduledReportRepository.findAllByCreatorIdWithDetails(creatorId);
  }

  @Override
  public List<ScheduledReport> findAllDueScheduledReports(LocalDateTime now) {
    return scheduledReportRepository.findAllDueWithDetails(now);
  }
}
