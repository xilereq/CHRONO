package pl.chrono.timemanager.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;
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
    when(timeData.findPendingCorrectionBefore(any())).thenReturn(Collections.emptyList());
    when(timeData.findUnsettledWorkLogs()).thenReturn(Collections.emptyList());

    scheduleService.processWorkLogs();

    verify(logRepository, never()).save(any());
  }

  @Test
  void flagUnclosedWorkLogs_FlagsLogAndClosesLeaveLogs() {
    WorkLog spyLog = spy(new WorkLog());
    spyLog.setWorkLogId(100);
    spyLog.setStartTime(LocalDateTime.now().minusHours(8));
    spyLog.setUser(spyUser);

    LeaveLog openBreak = new LeaveLog();
    openBreak.setStartTime(LocalDateTime.now().minusHours(2));
    openBreak.setEndTime(null);
    List<LeaveLog> leaves = new ArrayList<>();
    leaves.add(openBreak);
    spyLog.setLeaveLogs(leaves);

    when(timeData.findActiveWorkLogsBefore(any())).thenReturn(List.of(spyLog));
    when(timeData.findPendingCorrectionBefore(any())).thenReturn(Collections.emptyList());
    when(timeData.findUnsettledWorkLogs()).thenReturn(Collections.emptyList());

    scheduleService.processWorkLogs();

    assertThat(openBreak.getEndTime()).isNotNull();

    verify(spyLog).setPendingCorrection(true);
    verify(timeData).saveWorkLog(spyLog);

    ArgumentCaptor<LawViolationEvent> eventCaptor = ArgumentCaptor.forClass(LawViolationEvent.class);
    verify(eventPublisher).publishEvent(eventCaptor.capture());
    assertThat(eventCaptor.getValue().violationType()).isEqualTo("UNCLOSED_WORK_LOG");

    ArgumentCaptor<OperationLog> logCaptor = ArgumentCaptor.forClass(OperationLog.class);
    verify(logRepository).save(logCaptor.capture());
    assertThat(logCaptor.getValue().getDetails()).contains("1 flagged for correction");
  }

  @Test
  void flagUnclosedWorkLogs_WithNullStartTime() {
    WorkLog spyLog = spy(new WorkLog());
    spyLog.setWorkLogId(100);
    spyLog.setStartTime(null);
    spyLog.setUser(spyUser);

    when(timeData.findActiveWorkLogsBefore(any())).thenReturn(List.of(spyLog));
    when(timeData.findPendingCorrectionBefore(any())).thenReturn(Collections.emptyList());
    when(timeData.findUnsettledWorkLogs()).thenReturn(Collections.emptyList());

    scheduleService.processWorkLogs();

    verify(spyLog).setPendingCorrection(true);
    verify(timeData).saveWorkLog(spyLog);
    verify(eventPublisher).publishEvent(any(LawViolationEvent.class));
  }

  @Test
  void removeAbandonedWorkLogs_RegistersDebtAndDeletesLog() {
    WorkLog spyLog = spy(new WorkLog());
    spyLog.setWorkLogId(101);
    spyLog.setStartTime(LocalDateTime.now().minusWeeks(3));
    spyLog.setUser(spyUser);

    when(timeData.findActiveWorkLogsBefore(any())).thenReturn(Collections.emptyList());
    when(timeData.findPendingCorrectionBefore(any())).thenReturn(List.of(spyLog));
    when(timeData.findUnsettledWorkLogs()).thenReturn(Collections.emptyList());

    when(workTimeCalculationService.getBaseRequiredSeconds(any(), any())).thenReturn(8 * 3600);

    scheduleService.processWorkLogs();

    verify(overtimeService).registerPenaltyDebt(eq(spyUser), any(LocalDate.class), eq(8 * 3600));

    ArgumentCaptor<LawViolationEvent> eventCaptor = ArgumentCaptor.forClass(LawViolationEvent.class);
    verify(eventPublisher).publishEvent(eventCaptor.capture());
    assertThat(eventCaptor.getValue().violationType()).isEqualTo("ABANDONED_WORK_LOG");

    verify(timeData).deleteWorkLog(spyLog);

    ArgumentCaptor<OperationLog> logCaptor = ArgumentCaptor.forClass(OperationLog.class);
    verify(logRepository).save(logCaptor.capture());
    assertThat(logCaptor.getValue().getDetails()).contains("1 abandoned and removed");
  }

  @Test
  void removeAbandonedWorkLogs_NoDebtIfNoRequiredSeconds() {
    WorkLog spyLog = spy(new WorkLog());
    spyLog.setWorkLogId(102);
    spyLog.setStartTime(LocalDateTime.now().minusWeeks(3));
    spyLog.setUser(spyUser);

    when(timeData.findActiveWorkLogsBefore(any())).thenReturn(Collections.emptyList());
    when(timeData.findPendingCorrectionBefore(any())).thenReturn(List.of(spyLog));
    when(timeData.findUnsettledWorkLogs()).thenReturn(Collections.emptyList());

    when(workTimeCalculationService.getBaseRequiredSeconds(any(), any())).thenReturn(0);

    scheduleService.processWorkLogs();

    verify(overtimeService, never()).registerPenaltyDebt(any(User.class), any(), any(Integer.class));
    verify(eventPublisher, never()).publishEvent(any(LawViolationEvent.class));

    verify(timeData).deleteWorkLog(spyLog);
  }

  @Test
  void settleClosedWorkLogs_ExactWorkTimeTest() {
    WorkLog spyLog = spy(new WorkLog());
    spyLog.setStartTime(LocalDateTime.now().minusHours(8));
    spyLog.setUser(spyUser);

    when(timeData.findActiveWorkLogsBefore(any())).thenReturn(Collections.emptyList());
    when(timeData.findPendingCorrectionBefore(any())).thenReturn(Collections.emptyList());
    when(timeData.findUnsettledWorkLogs()).thenReturn(List.of(spyLog));

    when(workTimeCalculationService.getBaseRequiredSeconds(any(), any())).thenReturn(8 * 3600);
    when(workTimeCalculationService.getRoundingSeconds(any())).thenReturn(0);
    doReturn(8 * 3600L).when(spyLog).getEffectiveWorkTime(any(), any(Integer.class), any(Integer.class));

    scheduleService.processWorkLogs();

    verify(spyLog).setSettled(true);
    verify(timeData).saveWorkLog(spyLog);
    verify(overtimeService, never()).grantOvertime(any(), any());
    verify(overtimeService, never()).registerDebt(any(), any());

    ArgumentCaptor<OperationLog> logCaptor = ArgumentCaptor.forClass(OperationLog.class);
    verify(logRepository).save(logCaptor.capture());
    assertThat(logCaptor.getValue().getDetails()).contains("1 settled");
  }

  @Test
  void settleClosedWorkLogs_OvertimeEarnWithApplicationTest() {
    WorkLog spyLog = spy(new WorkLog());
    spyLog.setStartTime(LocalDateTime.now().minusHours(10));
    spyLog.setUser(spyUser);

    when(timeData.findActiveWorkLogsBefore(any())).thenReturn(Collections.emptyList());
    when(timeData.findPendingCorrectionBefore(any())).thenReturn(Collections.emptyList());
    when(timeData.findUnsettledWorkLogs()).thenReturn(List.of(spyLog));

    when(workTimeCalculationService.getBaseRequiredSeconds(any(), any())).thenReturn(8 * 3600);
    when(workTimeCalculationService.getRoundingSeconds(any())).thenReturn(0);
    doReturn(10 * 3600L).when(spyLog).getEffectiveWorkTime(any(), any(Integer.class), any(Integer.class));

    Application app = new Application();
    app.setType(ApplicationType.OVERTIME_EARN);
    when(applicationRepository.findApprovedByEmployeeAndDate(any(), any())).thenReturn(List.of(app));

    scheduleService.processWorkLogs();

    verify(overtimeService).grantOvertime(app, spyLog);
    verify(spyLog).setSettled(true);
  }

  @Test
  void settleClosedWorkLogs_ShortageCoveredWithEarlyExitTest() {
    WorkLog spyLog = spy(new WorkLog());
    spyLog.setStartTime(LocalDateTime.now().minusHours(6));
    spyLog.setUser(spyUser);

    when(timeData.findActiveWorkLogsBefore(any())).thenReturn(Collections.emptyList());
    when(timeData.findPendingCorrectionBefore(any())).thenReturn(Collections.emptyList());
    when(timeData.findUnsettledWorkLogs()).thenReturn(List.of(spyLog));

    when(workTimeCalculationService.getBaseRequiredSeconds(any(), any())).thenReturn(8 * 3600);
    when(workTimeCalculationService.getRoundingSeconds(any())).thenReturn(0);
    doReturn(6 * 3600L).when(spyLog).getEffectiveWorkTime(any(), any(Integer.class), any(Integer.class));

    Application app = new Application();
    app.setType(ApplicationType.EARLY_EXIT);
    app.setDurationSeconds(2 * 3600);
    when(applicationRepository.findApprovedByEmployeeAndDate(any(), any())).thenReturn(List.of(app));

    scheduleService.processWorkLogs();

    verify(overtimeService).registerDebt(app, spyLog);
    verify(overtimeService, never()).registerPenaltyDebt(any(), any(Integer.class));
    verify(spyLog).setSettled(true);
  }

  @Test
  void settleClosedWorkLogs_ShortageCoveredWithOvertimeTakeTest() {
    WorkLog spyLog = spy(new WorkLog());
    spyLog.setStartTime(LocalDateTime.now().minusHours(6));
    spyLog.setUser(spyUser);

    when(timeData.findActiveWorkLogsBefore(any())).thenReturn(Collections.emptyList());
    when(timeData.findPendingCorrectionBefore(any())).thenReturn(Collections.emptyList());
    when(timeData.findUnsettledWorkLogs()).thenReturn(List.of(spyLog));

    when(workTimeCalculationService.getBaseRequiredSeconds(any(), any())).thenReturn(8 * 3600);
    when(workTimeCalculationService.getRoundingSeconds(any())).thenReturn(0);
    doReturn(6 * 3600L).when(spyLog).getEffectiveWorkTime(any(), any(Integer.class), any(Integer.class));

    Application app = new Application();
    app.setType(ApplicationType.OVERTIME_TAKE);
    app.setDurationSeconds(2 * 3600);
    when(applicationRepository.findApprovedByEmployeeAndDate(any(), any())).thenReturn(List.of(app));

    scheduleService.processWorkLogs();

    verify(overtimeService).changeOvertimeStatus(app, spyLog);
    verify(overtimeService, never()).registerPenaltyDebt(any(), any(Integer.class));
    verify(spyLog).setSettled(true);
  }

  @Test
  void settleClosedWorkLogs_PenaltyForUnexcusedShortageTest() {
    WorkLog spyLog = spy(new WorkLog());
    spyLog.setWorkLogId(100);
    spyLog.setStartTime(LocalDateTime.now().minusHours(6));
    spyLog.setUser(spyUser);

    when(timeData.findActiveWorkLogsBefore(any())).thenReturn(Collections.emptyList());
    when(timeData.findPendingCorrectionBefore(any())).thenReturn(Collections.emptyList());
    when(timeData.findUnsettledWorkLogs()).thenReturn(List.of(spyLog));

    when(workTimeCalculationService.getBaseRequiredSeconds(any(), any())).thenReturn(8 * 3600);
    when(workTimeCalculationService.getRoundingSeconds(any())).thenReturn(0);
    doReturn(6 * 3600L).when(spyLog).getEffectiveWorkTime(any(), any(Integer.class), any(Integer.class));

    when(applicationRepository.findApprovedByEmployeeAndDate(any(), any())).thenReturn(Collections.emptyList());

    scheduleService.processWorkLogs();

    verify(overtimeService).registerPenaltyDebt(spyLog, 2 * 3600);

    ArgumentCaptor<LawViolationEvent> eventCaptor = ArgumentCaptor.forClass(LawViolationEvent.class);
    verify(eventPublisher).publishEvent(eventCaptor.capture());
    assertThat(eventCaptor.getValue().violationType()).isEqualTo("VIOLATION_UNEXCUSED_ABSENCE");
  }

  @Test
  void processOvertimeExpirationsTest() {
    when(userRepository.findAllUserIds()).thenReturn(List.of(1, 2, 3));

    scheduleService.processOvertimeExpirations();

    verify(overtimeService, times(3)).expireUnusedOvertime(any(Integer.class));

    ArgumentCaptor<OperationLog> logCaptor = ArgumentCaptor.forClass(OperationLog.class);
    verify(logRepository).save(logCaptor.capture());
    assertThat(logCaptor.getValue().getDetails()).contains("3 users");
  }

  // ==========================================
  // JACOCO BRANCH COVERAGE TESTS
  // ==========================================

  @Test
  void removeAbandonedWorkLogs_NullStartTime_TakesCurrentDateAndDeletes() {
    // Celuje w: workLog.getStartTime() != null ? ... : now.toLocalDate();
    WorkLog spyLog = spy(new WorkLog());
    spyLog.setWorkLogId(999);
    spyLog.setStartTime(null); // Brakujący branch
    spyLog.setUser(spyUser);

    when(timeData.findActiveWorkLogsBefore(any())).thenReturn(Collections.emptyList());
    when(timeData.findPendingCorrectionBefore(any())).thenReturn(List.of(spyLog));
    when(timeData.findUnsettledWorkLogs()).thenReturn(Collections.emptyList());

    // Zwracamy 0, żeby nie komplikować testu karą, chcemy tylko sprawdzić null-check
    when(workTimeCalculationService.getBaseRequiredSeconds(any(), any())).thenReturn(0);

    scheduleService.processWorkLogs();

    verify(timeData).deleteWorkLog(spyLog);
  }

  @Test
  void flagUnclosedWorkLogs_WithClosedBreak_IgnoresClosedBreak() {
    LocalDateTime referenceTime = LocalDateTime.now();
    LocalDateTime expectedEndTime = referenceTime.minusHours(3);

    WorkLog spyLog = spy(new WorkLog());
    spyLog.setWorkLogId(104);
    spyLog.setStartTime(referenceTime.minusHours(8));
    spyLog.setUser(spyUser);

    LeaveLog closedBreak = new LeaveLog();
    closedBreak.setStartTime(referenceTime.minusHours(4));

    closedBreak.setEndTime(expectedEndTime);

    List<LeaveLog> leaves = new ArrayList<>();
    leaves.add(closedBreak);
    spyLog.setLeaveLogs(leaves);

    when(timeData.findActiveWorkLogsBefore(any())).thenReturn(List.of(spyLog));
    when(timeData.findPendingCorrectionBefore(any())).thenReturn(Collections.emptyList());
    when(timeData.findUnsettledWorkLogs()).thenReturn(Collections.emptyList());

    scheduleService.processWorkLogs();

    assertThat(closedBreak.getEndTime()).isEqualTo(expectedEndTime);
  }

  @Test
  void settleClosedWorkLogs_OvertimeWithoutEarnApplication_IgnoresOtherApplications() {
    WorkLog spyLog = spy(new WorkLog());
    spyLog.setStartTime(LocalDateTime.now().minusHours(10));
    spyLog.setUser(spyUser);

    when(timeData.findActiveWorkLogsBefore(any())).thenReturn(Collections.emptyList());
    when(timeData.findPendingCorrectionBefore(any())).thenReturn(Collections.emptyList());
    when(timeData.findUnsettledWorkLogs()).thenReturn(List.of(spyLog));

    when(workTimeCalculationService.getBaseRequiredSeconds(any(), any())).thenReturn(8 * 3600);
    when(workTimeCalculationService.getRoundingSeconds(any())).thenReturn(0);
    doReturn(10 * 3600L).when(spyLog).getEffectiveWorkTime(any(), any(Integer.class), any(Integer.class));

    Application app = new Application();
    app.setType(ApplicationType.EARLY_EXIT);
    when(applicationRepository.findApprovedByEmployeeAndDate(any(), any())).thenReturn(List.of(app));

    scheduleService.processWorkLogs();

    verify(overtimeService, never()).grantOvertime(any(), any());
    verify(spyLog).setSettled(true);
  }

  @Test
  void settleClosedWorkLogs_ShortageWithNonCoveringApplication_CalculatesUnexcusedPenalty() {
    WorkLog spyLog = spy(new WorkLog());
    spyLog.setWorkLogId(105);
    spyLog.setStartTime(LocalDateTime.now().minusHours(6));
    spyLog.setUser(spyUser);

    when(timeData.findActiveWorkLogsBefore(any())).thenReturn(Collections.emptyList());
    when(timeData.findPendingCorrectionBefore(any())).thenReturn(Collections.emptyList());
    when(timeData.findUnsettledWorkLogs()).thenReturn(List.of(spyLog));

    when(workTimeCalculationService.getBaseRequiredSeconds(any(), any())).thenReturn(8 * 3600);
    when(workTimeCalculationService.getRoundingSeconds(any())).thenReturn(0);
    doReturn(6 * 3600L).when(spyLog).getEffectiveWorkTime(any(), any(Integer.class), any(Integer.class));

    Application app = new Application();
    app.setType(ApplicationType.OVERTIME_EARN);
    when(applicationRepository.findApprovedByEmployeeAndDate(any(), any())).thenReturn(List.of(app));

    scheduleService.processWorkLogs();

    verify(overtimeService).registerPenaltyDebt(spyLog, 2 * 3600);
  }

  @Test
  void settleClosedWorkLogs_ShortageCoveredWithAppNullDuration_CalculatesFullPenalty() {
    WorkLog spyLog = spy(new WorkLog());
    spyLog.setWorkLogId(106);
    spyLog.setStartTime(LocalDateTime.now().minusHours(6));
    spyLog.setUser(spyUser);

    when(timeData.findActiveWorkLogsBefore(any())).thenReturn(Collections.emptyList());
    when(timeData.findPendingCorrectionBefore(any())).thenReturn(Collections.emptyList());
    when(timeData.findUnsettledWorkLogs()).thenReturn(List.of(spyLog));

    when(workTimeCalculationService.getBaseRequiredSeconds(any(), any())).thenReturn(8 * 3600);
    when(workTimeCalculationService.getRoundingSeconds(any())).thenReturn(0);
    doReturn(6 * 3600L).when(spyLog).getEffectiveWorkTime(any(), any(Integer.class), any(Integer.class));

    Application app = new Application();
    app.setType(ApplicationType.EARLY_EXIT);
    app.setDurationSeconds(null);
    when(applicationRepository.findApprovedByEmployeeAndDate(any(), any())).thenReturn(List.of(app));

    scheduleService.processWorkLogs();

    verify(overtimeService).registerDebt(app, spyLog);
    verify(overtimeService).registerPenaltyDebt(spyLog, 2 * 3600);
  }
}
