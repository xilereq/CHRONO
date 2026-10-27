package pl.chrono.timemanager.api;

import java.time.LocalDate;
import pl.chrono.dbmanager.model.Policy;

public interface I_WorkTimeCalculationService {
  int getBaseRequiredSeconds(LocalDate date, Policy policy);

  int getRoundingSeconds(Policy policy);
}
