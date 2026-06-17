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
import pl.chrono.dbmanager.repository.OperationLogRepository;

@ExtendWith(MockitoExtension.class)
class WorkTaskSchedulerTest {

  @Mock
  private ScheduleService scheduleService;

  @Mock
  private OperationLogRepository operationLogRepository;

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
    when(operationLogRepository.existsByActionAndTimestampBetween(
        eq("SYSTEM_EXPIRE_OVERTIME"),
        any(LocalDateTime.class), any(LocalDateTime.class)
    )).thenReturn(true);

    workTaskScheduler.runOnStartup();

    verify(scheduleService, times(1)).processWorkLogs();
    verify(scheduleService, times(0)).processOvertimeExpirations();
  }

  @Test
  void overtimeExpirationFailedThisQuarterTest() {
    when(operationLogRepository.existsByActionAndTimestampBetween(
        eq("SYSTEM_EXPIRE_OVERTIME"),
        any(LocalDateTime.class), any(LocalDateTime.class)
    )).thenReturn(false);

    workTaskScheduler.runOnStartup();

    verify(scheduleService, times(1)).processWorkLogs();
    verify(scheduleService, times(1)).processOvertimeExpirations();
  }
}
