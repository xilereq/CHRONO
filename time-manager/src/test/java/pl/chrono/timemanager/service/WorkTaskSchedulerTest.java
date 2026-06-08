package pl.chrono.timemanager.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class WorkTaskSchedulerTest {

  @Mock
  private ScheduleService scheduleService;

  @InjectMocks
  private WorkTaskScheduler workTaskScheduler;

  @Test
  void runCountOvertimeTest() {
    workTaskScheduler.runCountOvertime();
    verify(scheduleService, times(1)).processWorkLogs();
  }

  @Test
  void runOnStartupTest() {
    workTaskScheduler.runOnStartup();
    verify(scheduleService, times(1)).processWorkLogs();
  }
}
