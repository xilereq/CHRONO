package pl.chrono.timemanager.api;

import java.time.LocalDate;
import pl.chrono.dbmanager.model.enums.ApplicationType;

public interface I_ApplicationValidationService {
  void validateApplicationLimits(
      Integer employeeId,
      LocalDate startDate,
      LocalDate endDate,
      ApplicationType type,
      Integer durationSeconds);
}
