package pl.chrono.dbmanager.model.enums;

public enum ScheduledReportType {
  EMPLOYEE_TIMESHEET,
  EMPLOYEE_OVERTIME,
  TEAM_WORKLOAD,
  TEAM_ABSENCES,
  TEAM_OVERTIME,
  TEAM_ANOMALY;

  public boolean isTeamReport() {
    return this == TEAM_WORKLOAD || this == TEAM_ABSENCES
        || this == TEAM_OVERTIME || this == TEAM_ANOMALY;
  }
}
