package pl.chrono.timemanager.dto;


public record WorkStats(
  int totalWorkTime,
  int remainingWorkTime,
  int totalMonthlyHours,
  int overtimeSeconds
) {}
