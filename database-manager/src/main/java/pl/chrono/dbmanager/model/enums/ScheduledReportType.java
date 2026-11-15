package pl.chrono.dbmanager.model.enums;

public enum ScheduledReportType {
  EMPLOYEE_TIMESHEET,
  EMPLOYEE_OVERTIME,
  TEAM_WORKLOAD,
  TEAM_ABSENCES,
  TEAM_OVERTIME,
  TEAM_ANOMALY,
  TEAM_MATRIX,
  TEAM_DAILY_OVERTIME,
  TEAM_NIGHT_HOURS;

  public boolean isTeamReport() {
    return this != EMPLOYEE_TIMESHEET && this != EMPLOYEE_OVERTIME;
  }
}
