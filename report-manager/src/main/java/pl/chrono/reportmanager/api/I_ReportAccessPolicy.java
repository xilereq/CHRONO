package pl.chrono.reportmanager.api;

public interface I_ReportAccessPolicy {

  void checkCanViewEmployee(Integer actorId, Integer employeeId);
}
