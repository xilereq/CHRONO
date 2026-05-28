package pl.chrono.reportmanager.service;

import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.chrono.dbmanager.model.Application;
import pl.chrono.dbmanager.repository.ApplicationRepository;
import pl.chrono.reportmanager.dto.TeamAbsenceEntry;
import pl.chrono.reportmanager.dto.TeamAbsencesReport;

@Service
@RequiredArgsConstructor
public class TeamAbsenceReportService {

  private final ApplicationRepository applicationRepository;

  @Transactional(readOnly = true)
  public TeamAbsencesReport generateReport(Integer managerId, LocalDate startDate, LocalDate endDate) {
    List<Application> apps = applicationRepository.findByEmployeeManagerUserIdAndStartDateBetween(managerId, startDate, endDate);

    List<TeamAbsenceEntry> entries = apps.stream()
        .map(app -> new TeamAbsenceEntry(
            app.getEmployee().getUserId(),
            app.getEmployee().getName() + " " + app.getEmployee().getSurname(),
            app.getType().name(),
            app.getStatus().name(),
            app.getStartDate(),
            app.getEndDate()
        )).toList();

    return new TeamAbsencesReport(managerId, startDate, endDate, entries);
  }
}
