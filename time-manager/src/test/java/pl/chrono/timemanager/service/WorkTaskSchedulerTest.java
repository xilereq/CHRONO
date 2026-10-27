package pl.chrono.timemanager.service;

import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import pl.chrono.dbmanager.ports.I_OperationLogData;

@ExtendWith(MockitoExtension.class)
class WorkTaskSchedulerTest {

  @Mock
  private ScheduleService scheduleService;

  @Mock
  private I_OperationLogData operationLogData;

  @InjectMocks
  private WorkTaskScheduler workTaskScheduler;

  @Test
  void runCountOvertimeTest() {
    workTaskScheduler.runCountOvertime();
    verify(scheduleService, times(1)).processWorkLogs();
  }

  @Test
  void runExpireOvertimeTest() {
    workTaskScheduler.runExpireOvertime();
    verify(scheduleService, times(1)).processOvertimeExpirations();
  }

  @Test
  void overtimeExpirationDoneThisQuarterTest() {
    when(operationLogData.isLogPresent(
        eq("SYSTEM_EXPIRE_OVERTIME"),
        any(LocalDateTime.class), any(LocalDateTime.class)
    )).thenReturn(true);

    workTaskScheduler.runOnStartup();

    verify(scheduleService, times(1)).processWorkLogs();
    verify(scheduleService, times(0)).processOvertimeExpirations();
  }

  @Test
  void overtimeExpirationFailedThisQuarterTest() {
    when(operationLogData.isLogPresent(
        eq("SYSTEM_EXPIRE_OVERTIME"),
        any(LocalDateTime.class), any(LocalDateTime.class)
    )).thenReturn(false);

    workTaskScheduler.runOnStartup();

    verify(scheduleService, times(1)).processWorkLogs();
    verify(scheduleService, times(1)).processOvertimeExpirations();
  }
}
