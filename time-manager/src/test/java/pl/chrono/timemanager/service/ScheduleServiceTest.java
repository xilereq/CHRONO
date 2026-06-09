package pl.chrono.timemanager.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import pl.chrono.dbmanager.model.Application;
import pl.chrono.dbmanager.model.OperationLog;
import pl.chrono.dbmanager.model.User;
import pl.chrono.dbmanager.model.WorkLog;
import pl.chrono.dbmanager.model.enums.ApplicationType;
import pl.chrono.dbmanager.ports.I_TimeData;
import pl.chrono.dbmanager.repository.ApplicationRepository;
import pl.chrono.dbmanager.repository.OperationLogRepository;

@ExtendWith(MockitoExtension.class)
class ScheduleServiceTest {

  @Mock
  private I_TimeData timeData;
  @Mock
  private OperationLogRepository logRepository;
  @Mock
  private OvertimeService overtimeService;
  @Mock
  private ApplicationRepository applicationRepository;

  @InjectMocks
  private ScheduleService scheduleService;

  private WorkLog createMockWorkLog(LocalDateTime endTime, long calculatedTime, int maxAllowed,
                                    long effectiveTime, int requiredTime,
                                    List<Application> dailyApps) {
    WorkLog mockWorkLog = mock(WorkLog.class);
    User mockUser = mock(User.class);

    lenient().when(mockWorkLog.getUser()).thenReturn(mockUser);
    lenient().when(mockUser.getUserId()).thenReturn(1);
    lenient().when(mockWorkLog.getStartTime()).thenReturn(LocalDateTime.now().minusHours(10));

    lenient().when(mockWorkLog.getEffectiveWorkTime(any(LocalDateTime.class)))
        .thenReturn(effectiveTime);
    lenient().when(mockWorkLog.getRequiredSeconds()).thenReturn(requiredTime);

    when(mockWorkLog.getEndTime()).thenReturn(endTime);

    if (endTime == null) {
      when(mockWorkLog.calculateWorkTime(any(LocalDateTime.class))).thenReturn(calculatedTime);
      when(mockWorkLog.getMaxAllowedSeconds()).thenReturn(maxAllowed);
    }

    lenient().when(applicationRepository.findApprovedByEmployeeAndDate(eq(1), any(LocalDate.class)))
        .thenReturn(dailyApps);

    return mockWorkLog;
  }

  @Test
  void emptyWorkLogListTest() {
    when(timeData.findUnsettledWorkLogs()).thenReturn(Collections.emptyList());

    scheduleService.processWorkLogs();

    verify(timeData, never()).saveWorkLog(any());
    verify(logRepository, never()).save(any());
  }

  @Test
  void forceCloseTest() {
    WorkLog mockWorkLog = createMockWorkLog(
        null, 50000L, 40000, 40000L, 40000, Collections.emptyList());

    when(timeData.findUnsettledWorkLogs()).thenReturn(List.of(mockWorkLog));

    scheduleService.processWorkLogs();

    verify(mockWorkLog).forceClose(any(LocalDateTime.class));
    verify(mockWorkLog).setSettled(true);
    verify(timeData).saveWorkLog(mockWorkLog);
    verify(logRepository).save(any(OperationLog.class));
  }

  @Test
  void grantOvertimeTest() {
    Application overtimeApp = new Application();
    overtimeApp.setType(ApplicationType.OVERTIME_EARN);

    WorkLog mockWorkLog = createMockWorkLog(
        LocalDateTime.now(), 0L, 0, 36000L, 28800, List.of(overtimeApp));

    when(timeData.findUnsettledWorkLogs()).thenReturn(List.of(mockWorkLog));

    scheduleService.processWorkLogs();

    verify(overtimeService).grantOvertime(overtimeApp, mockWorkLog);
  }

  @Test
  void registerDebtTest() {
    Application earlyExitApp = new Application();
    earlyExitApp.setType(ApplicationType.EARLY_EXIT);

    WorkLog mockWorkLog = createMockWorkLog(
        LocalDateTime.now(), 0L, 0, 20000L, 28800, List.of(earlyExitApp));

    when(timeData.findUnsettledWorkLogs()).thenReturn(List.of(mockWorkLog));

    scheduleService.processWorkLogs();

    verify(overtimeService).registerDebt(earlyExitApp, mockWorkLog);
  }

  @Test
  void changeOvertimeStatusTest() {
    Application overtimeTakeApp = new Application();
    overtimeTakeApp.setType(ApplicationType.OVERTIME_TAKE);

    WorkLog mockWorkLog = createMockWorkLog(
        LocalDateTime.now(), 0L, 0, 20000L, 28800, List.of(overtimeTakeApp));

    when(timeData.findUnsettledWorkLogs()).thenReturn(List.of(mockWorkLog));

    scheduleService.processWorkLogs();

    verify(overtimeService).changeOvertimeStatus(overtimeTakeApp, mockWorkLog);
  }

  @Test
  void maxTimeNotExceededTest() {
    WorkLog mockWorkLog = createMockWorkLog(null, 20000L, 40000, 0L, 0, Collections.emptyList());

    when(timeData.findUnsettledWorkLogs()).thenReturn(List.of(mockWorkLog));

    scheduleService.processWorkLogs();

    verify(mockWorkLog, never()).setSettled(true);
    verify(timeData, never()).saveWorkLog(any());
    verify(logRepository, never()).save(any());
  }

  @Test
  void grantNoOvertimeTest() {
    WorkLog mockWorkLog = createMockWorkLog(
        LocalDateTime.now(), 0L, 0, 36000L, 28800, Collections.emptyList());

    when(timeData.findUnsettledWorkLogs()).thenReturn(List.of(mockWorkLog));

    scheduleService.processWorkLogs();

    verify(overtimeService, never()).grantOvertime(any(), any());
  }

  @Test
  void noRegisterDebtTest() {
    WorkLog mockWorkLog = createMockWorkLog(
        LocalDateTime.now(), 0L, 0, 20000L, 28800, Collections.emptyList());

    when(timeData.findUnsettledWorkLogs()).thenReturn(List.of(mockWorkLog));

    scheduleService.processWorkLogs();

    verify(overtimeService, never()).registerDebt(any(), any());
    verify(overtimeService, never()).changeOvertimeStatus(any(), any());
  }

  @Test
  void overtimeWithEarlyExitApplicationTest() {
    Application wrongApp = new Application();
    wrongApp.setType(ApplicationType.EARLY_EXIT);

    WorkLog mockWorkLog = createMockWorkLog(
        LocalDateTime.now(), 0L, 0, 36000L, 28800, List.of(wrongApp));
    when(timeData.findUnsettledWorkLogs()).thenReturn(List.of(mockWorkLog));

    scheduleService.processWorkLogs();

    verify(overtimeService, never()).grantOvertime(any(), any());
  }

  @Test
  void earlyExitWithOvertimeEarnApplicationTest() {
    Application wrongApp = new Application();
    wrongApp.setType(ApplicationType.OVERTIME_EARN);

    WorkLog mockWorkLog = createMockWorkLog(
        LocalDateTime.now(), 0L, 0, 20000L, 28800, List.of(wrongApp));
    when(timeData.findUnsettledWorkLogs()).thenReturn(List.of(mockWorkLog));

    scheduleService.processWorkLogs();

    verify(overtimeService, never()).registerDebt(any(), any());
    verify(overtimeService, never()).changeOvertimeStatus(any(), any());
  }
}
