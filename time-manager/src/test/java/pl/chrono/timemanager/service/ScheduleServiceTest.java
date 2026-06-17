package pl.chrono.timemanager.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import pl.chrono.dbmanager.model.Application;
import pl.chrono.dbmanager.model.LeaveLog;
import pl.chrono.dbmanager.model.OperationLog;
import pl.chrono.dbmanager.model.Policy;
import pl.chrono.dbmanager.model.User;
import pl.chrono.dbmanager.model.WorkLog;
import pl.chrono.dbmanager.model.enums.ApplicationType;
import pl.chrono.dbmanager.ports.I_TimeData;
import pl.chrono.dbmanager.repository.ApplicationRepository;
import pl.chrono.dbmanager.repository.OperationLogRepository;
import pl.chrono.dbmanager.repository.UserRepository;
import pl.chrono.timemanager.event.LawViolationEvent;

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
  @Mock
  private ApplicationEventPublisher eventPublisher;
  @Mock
  private UserRepository userRepository;
  @Mock
  private WorkTimeCalculationService workTimeCalculationService;

  @InjectMocks
  private ScheduleService scheduleService;

  private User spyUser;

  @BeforeEach
  void setUp() {
    spyUser = spy(new User());
    spyUser.setUserId(1);

    lenient().doReturn(mock(Policy.class)).when(spyUser).getPolicyForDate(any());
  }


  @Test
  void processWorkLogsWithoutLogsTest() {
    when(timeData.findActiveWorkLogsBefore(any())).thenReturn(Collections.emptyList());
    when(timeData.findUnsettledWorkLogs()).thenReturn(Collections.emptyList());

    scheduleService.processWorkLogs();

    verify(logRepository, never()).save(any());
  }

  @Test
  void processWorkLogsActiveLogWithShortBreakTest() {
    WorkLog spyLog = spy(new WorkLog());
    spyLog.setStartTime(LocalDateTime.now().minusHours(8));
    spyLog.setUser(spyUser);

    LeaveLog openRecentBreak = new LeaveLog();
    openRecentBreak.setStartTime(LocalDateTime.now().minusHours(2));
    openRecentBreak.setEndTime(null);
    spyLog.setLeaveLogs(List.of(openRecentBreak));

    when(timeData.findActiveWorkLogsBefore(any())).thenReturn(List.of(spyLog));
    when(timeData.findUnsettledWorkLogs()).thenReturn(Collections.emptyList());

    doReturn(8 * 3600L).when(spyLog).calculateWorkTime(any());
    when(workTimeCalculationService.getMaxAllowedSeconds(any())).thenReturn(12 * 3600);

    scheduleService.processWorkLogs();

    verify(spyLog, never()).forceClose(any(), any(Integer.class));
    verify(timeData, never()).saveWorkLog(spyLog);
  }

  @Test
  void processWorkLogsActiveLogOverLimitTest() {
    WorkLog spyLog = spy(new WorkLog());
    spyLog.setWorkLogId(100);
    spyLog.setStartTime(LocalDateTime.now().minusHours(14));
    spyLog.setUser(spyUser);
    spyLog.setLeaveLogs(List.of());

    when(timeData.findActiveWorkLogsBefore(any())).thenReturn(List.of(spyLog));
    when(timeData.findUnsettledWorkLogs()).thenReturn(Collections.emptyList());

    doReturn(14 * 3600L).when(spyLog).calculateWorkTime(any());
    when(workTimeCalculationService.getMaxAllowedSeconds(any())).thenReturn(12 * 3600);
    when(workTimeCalculationService.getAdjustedRequiredSeconds(any(), any(), any())).thenReturn(
        8 * 3600);

    scheduleService.processWorkLogs();

    verify(spyLog).forceClose(any(), eq(8 * 3600));
    verify(timeData).saveWorkLog(spyLog);
    verify(eventPublisher).publishEvent(any(LawViolationEvent.class));
    verify(logRepository).save(any(OperationLog.class));
  }

  @Test
  void processWorkLogsActiveLogWithTooLongBreakTest() {
    WorkLog spyLog = spy(new WorkLog());
    spyLog.setWorkLogId(100);
    spyLog.setStartTime(LocalDateTime.now().minusHours(8));
    spyLog.setUser(spyUser);

    LeaveLog forgottenBreak = new LeaveLog();
    forgottenBreak.setStartTime(LocalDateTime.now().minusHours(5));
    forgottenBreak.setEndTime(null);
    spyLog.setLeaveLogs(List.of(forgottenBreak));

    when(timeData.findActiveWorkLogsBefore(any())).thenReturn(List.of(spyLog));
    when(timeData.findUnsettledWorkLogs()).thenReturn(Collections.emptyList());

    doReturn(3 * 3600L).when(spyLog).calculateWorkTime(any());
    when(workTimeCalculationService.getMaxAllowedSeconds(any())).thenReturn(12 * 3600);

    scheduleService.processWorkLogs();

    verify(spyLog).forceClose(any(), any(Integer.class));
  }

  @Test
  void processWorkLogsActiveLogNullStartTimeTest() {
    WorkLog spyLog = spy(new WorkLog());
    spyLog.setWorkLogId(100);
    spyLog.setStartTime(null);
    spyLog.setUser(spyUser);
    spyLog.setLeaveLogs(List.of());

    when(timeData.findActiveWorkLogsBefore(any())).thenReturn(List.of(spyLog));
    when(timeData.findUnsettledWorkLogs()).thenReturn(Collections.emptyList());

    doReturn(14 * 3600L).when(spyLog).calculateWorkTime(any());
    when(workTimeCalculationService.getMaxAllowedSeconds(any())).thenReturn(12 * 3600);

    scheduleService.processWorkLogs();

    verify(spyUser).getPolicyForDate(LocalDate.now());
  }

  @Test
  void processWorkLogsUnsettledLogsTest() {
    WorkLog spyLog = spy(new WorkLog());
    spyLog.setStartTime(LocalDateTime.now().minusHours(8));
    spyLog.setUser(spyUser);

    when(timeData.findActiveWorkLogsBefore(any())).thenReturn(Collections.emptyList());
    when(timeData.findUnsettledWorkLogs()).thenReturn(List.of(spyLog));
    when(workTimeCalculationService.getBaseRequiredSeconds(any(), any())).thenReturn(8 * 3600);

    doReturn(8 * 3600L).when(spyLog)
        .getEffectiveWorkTime(any(), any(Integer.class), any(Integer.class));

    scheduleService.processWorkLogs();

    verify(spyLog).setSettled(true);
    verify(overtimeService, never()).grantOvertime(any(), any());
    verify(overtimeService, never()).registerDebt(any(), any());
    verify(logRepository).save(any(OperationLog.class));
  }

  @Test
  void processWorkLogsOvertimeEarnWithApplicationTest() {
    WorkLog spyLog = spy(new WorkLog());
    spyLog.setStartTime(LocalDateTime.now().minusHours(10));
    spyLog.setUser(spyUser);

    when(timeData.findActiveWorkLogsBefore(any())).thenReturn(Collections.emptyList());
    when(timeData.findUnsettledWorkLogs()).thenReturn(List.of(spyLog));
    when(workTimeCalculationService.getBaseRequiredSeconds(any(), any())).thenReturn(8 * 3600);
    doReturn(10 * 3600L).when(spyLog)
        .getEffectiveWorkTime(any(), any(Integer.class), any(Integer.class));

    Application app = new Application();
    app.setType(ApplicationType.OVERTIME_EARN);
    when(applicationRepository.findApprovedByEmployeeAndDate(any(), any())).thenReturn(
        List.of(app));

    scheduleService.processWorkLogs();

    verify(overtimeService).grantOvertime(app, spyLog);
  }

  @Test
  void processWorkLogsShortageCoveredWithApplicationTest() {
    WorkLog spyLog = spy(new WorkLog());
    spyLog.setStartTime(LocalDateTime.now().minusHours(6));
    spyLog.setUser(spyUser);

    when(timeData.findActiveWorkLogsBefore(any())).thenReturn(Collections.emptyList());
    when(timeData.findUnsettledWorkLogs()).thenReturn(List.of(spyLog));
    when(workTimeCalculationService.getBaseRequiredSeconds(any(), any())).thenReturn(8 * 3600);
    doReturn(6 * 3600L).when(spyLog)
        .getEffectiveWorkTime(any(), any(Integer.class), any(Integer.class));

    Application app = new Application();
    app.setType(ApplicationType.EARLY_EXIT);
    app.setDurationSeconds(2 * 3600);
    when(applicationRepository.findApprovedByEmployeeAndDate(any(), any())).thenReturn(
        List.of(app));

    scheduleService.processWorkLogs();

    verify(overtimeService).registerDebt(app, spyLog);
    verify(overtimeService, never()).registerPenaltyDebt(any(), any(Integer.class));
  }

  @Test
  void processWorkLogsShortageCoveredWithOvertimeTakeTest() {
    WorkLog spyLog = spy(new WorkLog());
    spyLog.setStartTime(LocalDateTime.now().minusHours(6));
    spyLog.setUser(spyUser);

    when(timeData.findActiveWorkLogsBefore(any())).thenReturn(Collections.emptyList());
    when(timeData.findUnsettledWorkLogs()).thenReturn(List.of(spyLog));
    when(workTimeCalculationService.getBaseRequiredSeconds(any(), any())).thenReturn(8 * 3600);
    doReturn(6 * 3600L).when(spyLog)
        .getEffectiveWorkTime(any(), any(Integer.class), any(Integer.class));

    Application app = new Application();
    app.setType(ApplicationType.OVERTIME_TAKE);
    app.setDurationSeconds(2 * 3600);
    when(applicationRepository.findApprovedByEmployeeAndDate(any(), any())).thenReturn(
        List.of(app));

    scheduleService.processWorkLogs();

    verify(overtimeService).changeOvertimeStatus(app, spyLog);
    verify(overtimeService, never()).registerPenaltyDebt(any(), any(Integer.class));
  }

  @Test
  void processWorkLogsPenaltyForUnexcusedShortageTest() {
    WorkLog spyLog = spy(new WorkLog());
    spyLog.setWorkLogId(100);
    spyLog.setStartTime(LocalDateTime.now().minusHours(6));
    spyLog.setUser(spyUser);

    when(timeData.findActiveWorkLogsBefore(any())).thenReturn(Collections.emptyList());
    when(timeData.findUnsettledWorkLogs()).thenReturn(List.of(spyLog));
    when(workTimeCalculationService.getBaseRequiredSeconds(any(), any())).thenReturn(8 * 3600);
    doReturn(6 * 3600L).when(spyLog)
        .getEffectiveWorkTime(any(), any(Integer.class), any(Integer.class));

    when(applicationRepository.findApprovedByEmployeeAndDate(any(), any())).thenReturn(
        Collections.emptyList());

    scheduleService.processWorkLogs();

    verify(overtimeService).registerPenaltyDebt(spyLog, 2 * 3600);
    verify(eventPublisher).publishEvent(any(LawViolationEvent.class));
  }

  @Test
  void processWorkLogsNullApplicationDurationCausesShortagePenaltyTest() {
    WorkLog spyLog = spy(new WorkLog());
    spyLog.setWorkLogId(100);
    spyLog.setStartTime(LocalDateTime.now().minusHours(6));
    spyLog.setUser(spyUser);

    when(timeData.findActiveWorkLogsBefore(any())).thenReturn(Collections.emptyList());
    when(timeData.findUnsettledWorkLogs()).thenReturn(List.of(spyLog));
    when(workTimeCalculationService.getBaseRequiredSeconds(any(), any())).thenReturn(8 * 3600);
    doReturn(6 * 3600L).when(spyLog)
        .getEffectiveWorkTime(any(), any(Integer.class), any(Integer.class));

    Application app = new Application();
    app.setType(ApplicationType.EARLY_EXIT);
    app.setDurationSeconds(null);
    when(applicationRepository.findApprovedByEmployeeAndDate(any(), any())).thenReturn(
        List.of(app));

    scheduleService.processWorkLogs();

    verify(overtimeService).registerDebt(app, spyLog);
    verify(overtimeService).registerPenaltyDebt(spyLog, 2 * 3600);
  }

  @Test
  void processOvertimeExpirationsTest() {
    when(userRepository.findAllUserIds()).thenReturn(List.of(1, 2, 3));

    scheduleService.processOvertimeExpirations();

    verify(overtimeService, times(3)).expireUnusedOvertime(any(Integer.class));

    ArgumentCaptor<OperationLog> logCaptor = ArgumentCaptor.forClass(OperationLog.class);
    verify(logRepository).save(logCaptor.capture());
    assert (logCaptor.getValue().getDetails().contains("3 users"));
  }

  @Test
  void processWorkLogOvertimeWithWrongApplicationTest() {
    WorkLog spyLog = spy(new WorkLog());
    spyLog.setStartTime(LocalDateTime.now().minusHours(10));
    spyLog.setUser(spyUser);

    when(timeData.findUnsettledWorkLogs()).thenReturn(List.of(spyLog));
    when(workTimeCalculationService.getBaseRequiredSeconds(any(), any())).thenReturn(8 * 3600);
    doReturn(10 * 3600L).when(spyLog)
        .getEffectiveWorkTime(any(), any(Integer.class), any(Integer.class));

    Application wrongApp = new Application();
    wrongApp.setType(ApplicationType.EARLY_EXIT);
    when(applicationRepository.findApprovedByEmployeeAndDate(any(), any())).thenReturn(
        List.of(wrongApp));

    scheduleService.processWorkLogs();

    verify(overtimeService, never()).grantOvertime(any(), any());
  }

  @Test
  void processWorkLogsPenaltyForShortWorkTimeWithOvertimeEarnApplication() {
    WorkLog spyLog = spy(new WorkLog());
    spyLog.setWorkLogId(101);
    spyLog.setStartTime(LocalDateTime.now().minusHours(6));
    spyLog.setUser(spyUser);

    when(timeData.findUnsettledWorkLogs()).thenReturn(List.of(spyLog));
    when(workTimeCalculationService.getBaseRequiredSeconds(any(), any())).thenReturn(8 * 3600);
    doReturn(6 * 3600L).when(spyLog)
        .getEffectiveWorkTime(any(), any(Integer.class), any(Integer.class));

    Application wrongApp = new Application();
    wrongApp.setType(ApplicationType.OVERTIME_EARN);
    when(applicationRepository.findApprovedByEmployeeAndDate(any(), any())).thenReturn(
        List.of(wrongApp));

    scheduleService.processWorkLogs();

    verify(overtimeService).registerPenaltyDebt(spyLog, 2 * 3600);
  }

  @Test
  void processWorkLogsNoForgottenBreaksTest() {
    WorkLog spyLog = spy(new WorkLog());
    spyLog.setStartTime(LocalDateTime.now().minusHours(8));
    spyLog.setUser(spyUser);

    spyLog.setLeaveLogs(List.of());

    LeaveLog closedBreak = new LeaveLog();
    closedBreak.setEndTime(LocalDateTime.now().minusHours(1));

    LeaveLog shortOpenBreak = new LeaveLog();
    shortOpenBreak.setStartTime(LocalDateTime.now().minusHours(1));
    shortOpenBreak.setEndTime(null);

    spyLog.setLeaveLogs(List.of(closedBreak, shortOpenBreak));

    when(timeData.findActiveWorkLogsBefore(any())).thenReturn(List.of(spyLog));
    when(timeData.findUnsettledWorkLogs()).thenReturn(Collections.emptyList());

    doReturn(8 * 3600L).when(spyLog).calculateWorkTime(any());
    when(workTimeCalculationService.getMaxAllowedSeconds(any())).thenReturn(12 * 3600);

    scheduleService.processWorkLogs();

    verify(spyLog, never()).forceClose(any(), any(Integer.class));
  }
}
