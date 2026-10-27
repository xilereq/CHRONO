package pl.chrono.timemanager.api;

import pl.chrono.dbmanager.model.Application;

public interface I_OvertimeService {
  void reserveOvertimeForApplication(Application application);

  void refundOvertimeForApplication(Application application);
}
