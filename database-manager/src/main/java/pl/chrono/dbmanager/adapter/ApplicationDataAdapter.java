package pl.chrono.dbmanager.adapter;

import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import pl.chrono.dbmanager.model.Application;
import pl.chrono.dbmanager.model.enums.ApplicationStatus;
import pl.chrono.dbmanager.model.enums.ApplicationType;
import pl.chrono.dbmanager.ports.I_ApplicationData;
import pl.chrono.dbmanager.repository.ApplicationRepository;

@Component
@RequiredArgsConstructor
public class ApplicationDataAdapter implements I_ApplicationData {
  private final ApplicationRepository applicationRepository;

  @Override
  public List<Application> findApprovedApplicationsByEmployeeAndDate(
      Integer employeeId, LocalDate date) {
    return applicationRepository.findApprovedByEmployeeAndDate(employeeId, date);
  }

  @Override
  public List<Application> findApplicationsByEmployeeAndTypeAndDateRange(
      Integer employeeId,
      ApplicationType type,
      ApplicationStatus status,
      LocalDate startOfYear,
      LocalDate endOfYear) {
    return applicationRepository.findApplicationsByEmployeeAndTypeAndDateRange(
        employeeId, type, status, startOfYear, endOfYear);
  }
}
