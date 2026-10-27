package pl.chrono.dbmanager.ports;

import java.time.LocalDate;
import java.util.List;
import pl.chrono.dbmanager.model.Application;
import pl.chrono.dbmanager.model.enums.ApplicationStatus;
import pl.chrono.dbmanager.model.enums.ApplicationType;

public interface I_ApplicationData {
  List<Application> findApprovedApplicationsByEmployeeAndDate(Integer employeeId, LocalDate date);

  List<Application> findApplicationsByEmployeeAndTypeAndDateRange(
      Integer employeeId,
      ApplicationType type,
      ApplicationStatus status,
      LocalDate startOfYear,
      LocalDate endOfYear);
}
