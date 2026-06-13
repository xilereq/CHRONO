package pl.chrono.timemanager.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import pl.chrono.dbmanager.model.Application;
import pl.chrono.dbmanager.model.Policy;
import pl.chrono.dbmanager.model.User;
import pl.chrono.dbmanager.model.WorkLog;
import pl.chrono.dbmanager.model.enums.ApplicationType;
import pl.chrono.dbmanager.ports.I_TimeData;
import pl.chrono.dbmanager.repository.ApplicationRepository;
import pl.chrono.dbmanager.repository.UserRepository;
import pl.chrono.timemanager.dto.response.WorkLogDto;
import pl.chrono.timemanager.dto.response.WorkStats;
import pl.chrono.timemanager.event.LawViolationEvent;
import pl.chrono.timemanager.exception.UserException;
import pl.chrono.timemanager.exception.WorkException;
import pl.chrono.timemanager.mapper.WorkLogMapper;

@ExtendWith(MockitoExtension.class)
class WorkLogServiceTest {

  @Mock
  private I_TimeData timeData;
  @Mock
  private UserRepository userRepository;
  @Mock
  private WorkLogMapper workLogMapper;
  @Mock
  private OvertimeService overtimeService;
  @Mock
  private ApplicationRepository applicationRepository;
  @Mock
  private ApplicationEventPublisher eventPublisher;

  @InjectMocks
  private WorkLogService workLogService;


  private WorkLog setupActiveWorkLog(long workTimeSeconds, int requiredSeconds, int roundingSeconds,
                                     int maxAllowedSeconds) {
    WorkLog workLog = mock(WorkLog.class);
    when(timeData.findActiveWorkLog(1)).thenReturn(Optional.of(workLog));
    when(timeData.isUserOnLeave(1)).thenReturn(false);
    when(workLog.calculateWorkTime(any())).thenReturn(workTimeSeconds);
    when(workLog.getRequiredSeconds()).thenReturn(requiredSeconds);
    when(workLog.getRoundingSeconds()).thenReturn(roundingSeconds);
    when(workLog.getMaxAllowedSeconds()).thenReturn(maxAllowedSeconds);
    return workLog;
  }

  private WorkLog setupActiveWorkLogDefault(long workTimeSeconds) {
    WorkLog workLog = setupActiveWorkLog(workTimeSeconds, 28800, 900, 40000);
    when(applicationRepository.findApprovedByEmployeeAndDate(any(), any())).thenReturn(List.of());
    return workLog;
  }

  private Application buildApplication(ApplicationType type, Integer durationSeconds) {
    Application app = new Application();
    app.setType(type);
    app.setDurationSeconds(durationSeconds);
    return app;
  }

  private User setupUserWithPolicy(int requiredSecondsPerDay) {
    User user = mock(User.class);
    Policy policy = mock(Policy.class);
    when(userRepository.findById(1)).thenReturn(Optional.of(user));
    when(user.getActivePolicy()).thenReturn(policy);
    when(policy.getRequiredSecondsForDate(any(LocalDate.class))).thenReturn(requiredSecondsPerDay);
    return user;
  }

  private void setupEndWorkSuccessPath(WorkLog workLog) {
    User user = mock(User.class);
    when(workLog.getUser()).thenReturn(user);
    when(timeData.saveWorkLog(workLog)).thenReturn(workLog);
    when(workLogMapper.toDto(workLog)).thenReturn(new WorkLogDto());
  }

  @Test
  void startWorkAlreadyStartedExceptionTest() {
    when(timeData.isUserWorking(1)).thenReturn(true);
    assertThrows(WorkException.WorkStarted.class, () -> workLogService.startWork(1));
  }

  @Test
  void startWorkUserNotFoundTest() {
    when(timeData.isUserWorking(1)).thenReturn(false);
    when(userRepository.findById(1)).thenReturn(Optional.empty());
    assertThrows(UserException.NotFound.class, () -> workLogService.startWork(1));
  }

  @Test
  void startWorkTest() {
    WorkLog savedWorkLog = new WorkLog();
    WorkLogDto dto = new WorkLogDto();
    dto.setWorkLogId(1);

    when(timeData.isUserWorking(1)).thenReturn(false);
    when(timeData.findLastClosedWorkLog(1)).thenReturn(Optional.empty());
    when(userRepository.findById(1)).thenReturn(Optional.of(new User()));
    when(timeData.saveWorkLog(any(WorkLog.class))).thenReturn(savedWorkLog);
    when(workLogMapper.toDto(savedWorkLog)).thenReturn(dto);

    assertEquals(dto, workLogService.startWork(1));
    verify(timeData).saveWorkLog(any(WorkLog.class));
  }

  @Test
  void startWorkEventPublishTest() {
    WorkLog lastLog = mock(WorkLog.class);
    when(lastLog.getEndTime()).thenReturn(LocalDateTime.now().minusHours(5));
    when(lastLog.getWorkLogId()).thenReturn(99);

    when(timeData.isUserWorking(1)).thenReturn(false);
    when(timeData.findLastClosedWorkLog(1)).thenReturn(Optional.of(lastLog));
    when(userRepository.findById(1)).thenReturn(Optional.of(new User()));
    when(timeData.saveWorkLog(any())).thenReturn(new WorkLog());
    when(workLogMapper.toDto(any())).thenReturn(new WorkLogDto());

    workLogService.startWork(1);

    ArgumentCaptor<LawViolationEvent> captor = ArgumentCaptor.forClass(LawViolationEvent.class);
    verify(eventPublisher).publishEvent(captor.capture());
    assertEquals("VIOLATION_REST", captor.getValue().violationType());
  }

  @Test
  void startWorkNoEventPublishTest() {
    WorkLog lastLog = mock(WorkLog.class);
    when(lastLog.getEndTime()).thenReturn(LocalDateTime.now().minusHours(12));

    when(timeData.isUserWorking(1)).thenReturn(false);
    when(timeData.findLastClosedWorkLog(1)).thenReturn(Optional.of(lastLog));
    when(userRepository.findById(1)).thenReturn(Optional.of(new User()));
    when(timeData.saveWorkLog(any())).thenReturn(new WorkLog());
    when(workLogMapper.toDto(any())).thenReturn(new WorkLogDto());

    workLogService.startWork(1);

    verify(eventPublisher, never()).publishEvent(any());
  }

  @Test
  void endWorkNoActiveWorkTest() {
    when(timeData.findActiveWorkLog(1)).thenReturn(Optional.empty());
    assertThrows(WorkException.NoActiveWork.class, () -> workLogService.endWork(1));
  }

  @Test
  void endWorkUserOnLeaveExceptionTest() {
    WorkLog workLog = mock(WorkLog.class);
    when(timeData.findActiveWorkLog(1)).thenReturn(Optional.of(workLog));
    when(timeData.isUserOnLeave(1)).thenReturn(true);
    assertThrows(WorkException.LeaveStarted.class, () -> workLogService.endWork(1));
  }

  @Test
  void endWorkExcessiveWorkNotificationTest() {
    User user = new User();
    user.setUserId(1);
    WorkLog workLog = new WorkLog();
    workLog.setWorkLogId(1);
    workLog.setUser(user);
    workLog.setStartTime(LocalDateTime.now().minusHours(15));

    WorkLog spyLog = spy(workLog);
    when(timeData.findActiveWorkLog(1)).thenReturn(Optional.of(spyLog));
    when(timeData.isUserOnLeave(1)).thenReturn(false);
    when(timeData.saveWorkLog(any())).thenReturn(spyLog);
    doReturn(50000L).when(spyLog).calculateWorkTime(any());
    doReturn(40000).when(spyLog).getMaxAllowedSeconds();
    doReturn(28800).when(spyLog).getRequiredSeconds();
    doReturn(900).when(spyLog).getRoundingSeconds();

    workLogService.endWork(1);

    verify(spyLog).forceClose(any());
    verify(timeData).saveWorkLog(spyLog);
    verify(eventPublisher).publishEvent(any(LawViolationEvent.class));
  }

  @Test
  void endWorkInsufficientWorkTimeExceptionTest() {
    setupActiveWorkLogDefault(20000L);
    assertThrows(WorkException.InsufficientWorkTime.class, () -> workLogService.endWork(1));
  }

  @Test
  void endWorkTest() {
    WorkLog workLog = setupActiveWorkLogDefault(28000L);
    setupEndWorkSuccessPath(workLog);

    assertNotNull(workLogService.endWork(1));
    verify(workLog).setEndTime(any(LocalDateTime.class));
  }

  @Test
  void earlyExitApplicationReducesWorkTimeTest() {
    WorkLog workLog = setupActiveWorkLog(21000L, 28800, 900, 40000);
    when(applicationRepository.findApprovedByEmployeeAndDate(any(), any())).thenReturn(
        List.of(buildApplication(ApplicationType.EARLY_EXIT, 7200)));
    setupEndWorkSuccessPath(workLog);

    assertNotNull(workLogService.endWork(1));
  }

  @Test
  void overtimeTakeApplicationReducesWorkTimeTest() {
    WorkLog workLog = setupActiveWorkLog(21000L, 28800, 900, 40000);
    when(applicationRepository.findApprovedByEmployeeAndDate(any(), any())).thenReturn(
        List.of(buildApplication(ApplicationType.OVERTIME_TAKE, 7200)));
    setupEndWorkSuccessPath(workLog);

    assertNotNull(workLogService.endWork(1));
  }

  @Test
  void overtimeEarnApplicationIncreasesWorkTimeTest() {
    setupActiveWorkLog(28000L, 28800, 900, 40000);
    when(applicationRepository.findApprovedByEmployeeAndDate(any(), any())).thenReturn(
        List.of(buildApplication(ApplicationType.OVERTIME_EARN, 3600)));

    assertThrows(WorkException.InsufficientWorkTime.class, () -> workLogService.endWork(1));
  }

  @Test
  void nullDurationApplicationTest() {
    WorkLog workLog = setupActiveWorkLog(28000L, 28800, 900, 40000);
    when(applicationRepository.findApprovedByEmployeeAndDate(any(), any())).thenReturn(
        List.of(buildApplication(ApplicationType.EARLY_EXIT, null)));
    setupEndWorkSuccessPath(workLog);

    assertNotNull(workLogService.endWork(1));
  }

  @Test
  void otherApplicationTypeDoesNotChangeWorkTimeTest() {
    setupActiveWorkLog(20000L, 28800, 900, 40000);
    when(applicationRepository.findApprovedByEmployeeAndDate(any(), any())).thenReturn(
        List.of(buildApplication(ApplicationType.HOLIDAY_LEAVE, 3600)));

    assertThrows(WorkException.InsufficientWorkTime.class, () -> workLogService.endWork(1));
  }

  @Test
  void isUserWorkingTest() {
    when(timeData.isUserWorking(1)).thenReturn(true);
    assertTrue(workLogService.isUserWorking(1));
  }

  @Test
  void getActiveWorkTimeTest() {
    WorkLog workLog = mock(WorkLog.class);
    when(timeData.findActiveWorkLog(1)).thenReturn(Optional.of(workLog));
    when(workLog.calculateWorkTime(any())).thenReturn(3665L);

    assertEquals("01:01:05", workLogService.getActiveWorkTime(1));
  }

  @Test
  void getActiveWorkTimeZeroTest() {
    when(timeData.findActiveWorkLog(1)).thenReturn(Optional.empty());
    assertEquals("00:00:00", workLogService.getActiveWorkTime(1));
  }

  @Test
  void getMonthlyStatsUserNotFoundTest() {
    when(userRepository.findById(1)).thenReturn(Optional.empty());
    assertThrows(UserException.NotFound.class, () -> workLogService.getMonthlyStats(1));
  }

  @Test
  void getMonthlyStatsWithPolicyTest() {
    setupUserWithPolicy(28800);

    WorkLog pastLog = mock(WorkLog.class);
    when(pastLog.getEndTime()).thenReturn(LocalDate.now().minusDays(1).atStartOfDay());
    when(pastLog.getEffectiveWorkTime(any())).thenReturn(36000L);

    when(timeData.findUserWorkLogsAfter(eq(1), any(LocalDateTime.class))).thenReturn(
        List.of(pastLog));
    when(overtimeService.getOvertimeBalance(1)).thenReturn(7200);

    WorkStats result = workLogService.getMonthlyStats(1);

    assertNotNull(result);
    assertEquals(10, result.totalWorkTime());
    assertEquals(7200, result.overtimeSeconds());
    assertTrue(result.totalMonthlyHours() > 0);
  }

  @Test
  void getMonthlyStatsWithoutPolicyTest() {
    User user = mock(User.class);
    when(userRepository.findById(1)).thenReturn(Optional.of(user));
    when(user.getActivePolicy()).thenReturn(null);
    when(timeData.findUserWorkLogsAfter(eq(1), any(LocalDateTime.class))).thenReturn(List.of());
    when(overtimeService.getOvertimeBalance(1)).thenReturn(0);

    WorkStats result = workLogService.getMonthlyStats(1);

    assertNotNull(result);
    assertTrue(result.totalMonthlyHours() > 0);
  }

  @Test
  void getMonthlyStatsFiltersInCalculateTotalHoursWorkedTest() {
    User user = mock(User.class);
    when(userRepository.findById(1)).thenReturn(Optional.of(user));
    when(user.getActivePolicy()).thenReturn(null);

    LocalDate today = LocalDate.now();
    WorkLog validLog = mock(WorkLog.class);
    when(validLog.getEndTime()).thenReturn(today.minusDays(1).atStartOfDay());
    when(validLog.getEffectiveWorkTime(any())).thenReturn(36000L);

    WorkLog activeLog = mock(WorkLog.class);
    when(activeLog.getEndTime()).thenReturn(null);

    WorkLog todayLog = mock(WorkLog.class);
    when(todayLog.getEndTime()).thenReturn(today.atStartOfDay().plusHours(2));

    when(timeData.findUserWorkLogsAfter(eq(1), any(LocalDateTime.class))).thenReturn(
        List.of(validLog, activeLog, todayLog));
    when(overtimeService.getOvertimeBalance(1)).thenReturn(0);

    WorkStats result = workLogService.getMonthlyStats(1);

    assertNotNull(result);
    assertEquals(10, result.totalWorkTime());
  }

  @Test
  void zeroRequiredSecondsPolicyTest() {
    setupUserWithPolicy(0);
    when(timeData.findUserWorkLogsAfter(eq(1), any(LocalDateTime.class))).thenReturn(List.of());
    when(overtimeService.getOvertimeBalance(1)).thenReturn(0);

    WorkStats result = workLogService.getMonthlyStats(1);

    assertNotNull(result);
    assertEquals(0, result.totalMonthlyHours());
  }

  @Test
  void getTodayTargetWithDifferentApplicationsTest() {
    setupUserWithPolicy(28800);

    when(applicationRepository.findApprovedByEmployeeAndDate(
        eq(1),
        any(LocalDate.class)
    )).thenReturn(List.of(
        buildApplication(ApplicationType.EARLY_EXIT, 1800),
        buildApplication(ApplicationType.OVERTIME_TAKE, 3600),
        buildApplication(ApplicationType.OVERTIME_EARN, 7200),
        buildApplication(ApplicationType.HOLIDAY_LEAVE, null)
    ));

    assertEquals("08:30:00", workLogService.getTodayTarget(1));
  }

  @Test
  void getTodayTargetNegativeTargetTest() {
    User user = mock(User.class);
    when(userRepository.findById(1)).thenReturn(Optional.of(user));
    when(user.getActivePolicy()).thenReturn(null);
    when(applicationRepository.findApprovedByEmployeeAndDate(
        eq(1),
        any(LocalDate.class)
    )).thenReturn(List.of(buildApplication(ApplicationType.EARLY_EXIT, 72000)));

    assertEquals("00:00:00", workLogService.getTodayTarget(1));
  }

  @Test
  void getTodayTargetUserNotFoundExceptionTest() {
    when(userRepository.findById(1)).thenReturn(Optional.empty());
    assertThrows(UserException.NotFound.class, () -> workLogService.getTodayTarget(1));
  }

  @Test
  void getTodayTargetOnWeekendWithNoPolicyTest() {
    User user = mock(User.class);
    when(userRepository.findById(1)).thenReturn(Optional.of(user));
    when(user.getActivePolicy()).thenReturn(null);

    LocalDate saturday = LocalDate.of(2026, 6, 13);
    try (MockedStatic<LocalDate> mockedLocalDate = mockStatic(LocalDate.class)) {
      mockedLocalDate.when(LocalDate::now).thenReturn(saturday);
      when(applicationRepository.findApprovedByEmployeeAndDate(eq(1), eq(saturday))).thenReturn(
          List.of());

      assertEquals("00:00:00", workLogService.getTodayTarget(1));
    }
  }

  @Test
  void getTodayTargetDefaultPolicyTest() {
    User user = mock(User.class);
    when(userRepository.findById(1)).thenReturn(Optional.of(user));
    when(user.getActivePolicy()).thenReturn(null);

    LocalDate monday = LocalDate.of(2026, 6, 9);
    try (MockedStatic<LocalDate> mockedLocalDate = mockStatic(LocalDate.class)) {
      mockedLocalDate.when(LocalDate::now).thenReturn(monday);
      when(applicationRepository.findApprovedByEmployeeAndDate(eq(1), eq(monday))).thenReturn(
          List.of());

      assertEquals("08:00:00", workLogService.getTodayTarget(1));
    }
  }

  private WorkLog setupWeeklyLimitTest(int currentLogHours, int pastLogHours, int maxWeeklyHours) {
    WorkLog currentLog = mock(WorkLog.class);
    WorkLog pastLog = mock(WorkLog.class);
    User user = mock(User.class);
    Policy policy = mock(Policy.class);

    when(timeData.findActiveWorkLog(1)).thenReturn(Optional.of(currentLog));
    when(timeData.isUserOnLeave(1)).thenReturn(false);
    when(currentLog.getUser()).thenReturn(user);
    when(user.getActivePolicy()).thenReturn(policy);
    lenient().when(policy.getMaxWeeklyHours()).thenReturn(maxWeeklyHours);
    lenient().when(currentLog.getWorkLogId()).thenReturn(1);

    long currentSeconds = (long) currentLogHours * 3600;
    long pastSeconds = (long) pastLogHours * 3600;
    when(currentLog.getEffectiveWorkTime(any())).thenReturn(currentSeconds);
    when(pastLog.getEffectiveWorkTime(any())).thenReturn(pastSeconds);
    when(timeData.findUserWorkLogsAfter(any(), any())).thenReturn(List.of(currentLog, pastLog));

    when(currentLog.calculateWorkTime(any())).thenReturn(currentSeconds);
    when(currentLog.getRequiredSeconds()).thenReturn(4 * 3600);
    when(currentLog.getRoundingSeconds()).thenReturn(0);
    when(currentLog.getMaxAllowedSeconds()).thenReturn(12 * 3600);
    when(applicationRepository.findApprovedByEmployeeAndDate(any(), any())).thenReturn(List.of());
    when(timeData.saveWorkLog(currentLog)).thenReturn(currentLog);

    return currentLog;
  }

  @Test
  void weeklyLimitNotificationTest() {
    WorkLog currentLog = setupWeeklyLimitTest(5, 40, 40);
    when(workLogMapper.toDto(currentLog)).thenReturn(new WorkLogDto());

    workLogService.endWork(1);

    ArgumentCaptor<LawViolationEvent> eventCaptor = ArgumentCaptor.forClass(
        LawViolationEvent.class);
    verify(eventPublisher).publishEvent(eventCaptor.capture());
    assertEquals("VIOLATION_MAX_WEEKLY_WORK", eventCaptor.getValue().violationType());
    assertTrue(eventCaptor.getValue().message().contains("40 hours"));
  }

  @Test
  void weeklyLimitAlreadyExceededTest() {
    WorkLog currentLog = setupWeeklyLimitTest(5, 50, 40);
    when(workLogMapper.toDto(currentLog)).thenReturn(new WorkLogDto());

    workLogService.endWork(1);

    verify(eventPublisher, never()).publishEvent(any());
  }

  @Test
  void weeklyLimitNotExceededTest() {
    WorkLog currentLog = setupWeeklyLimitTest(4, 30, 40);
    when(workLogMapper.toDto(currentLog)).thenReturn(new WorkLogDto());

    workLogService.endWork(1);

    verify(eventPublisher, never()).publishEvent(any());
  }

  @Test
  void default48HoursWeeklyLimitTest() {
    WorkLog currentLog = mock(WorkLog.class);
    WorkLog pastLog = mock(WorkLog.class);
    User user = mock(User.class);
    Policy policy = mock(Policy.class);

    when(timeData.findActiveWorkLog(1)).thenReturn(Optional.of(currentLog));
    when(timeData.isUserOnLeave(1)).thenReturn(false);
    when(currentLog.getUser()).thenReturn(user);
    when(user.getActivePolicy()).thenReturn(policy);
    when(policy.getMaxWeeklyHours()).thenReturn(null);

    long currentSeconds = 5 * 3600L;
    long pastSeconds = 48 * 3600L;
    when(currentLog.getEffectiveWorkTime(any())).thenReturn(currentSeconds);
    when(pastLog.getEffectiveWorkTime(any())).thenReturn(pastSeconds);
    when(timeData.findUserWorkLogsAfter(any(), any())).thenReturn(List.of(currentLog, pastLog));
    lenient().when(currentLog.getWorkLogId()).thenReturn(1);

    when(currentLog.calculateWorkTime(any())).thenReturn(currentSeconds);
    when(currentLog.getRequiredSeconds()).thenReturn(4 * 3600);
    when(currentLog.getRoundingSeconds()).thenReturn(0);
    when(currentLog.getMaxAllowedSeconds()).thenReturn(12 * 3600);
    when(applicationRepository.findApprovedByEmployeeAndDate(any(), any())).thenReturn(List.of());
    when(timeData.saveWorkLog(currentLog)).thenReturn(currentLog);
    when(workLogMapper.toDto(currentLog)).thenReturn(new WorkLogDto());

    workLogService.endWork(1);

    ArgumentCaptor<LawViolationEvent> captor = ArgumentCaptor.forClass(LawViolationEvent.class);
    verify(eventPublisher).publishEvent(captor.capture());
    assertEquals("VIOLATION_MAX_WEEKLY_WORK", captor.getValue().violationType());
    assertTrue(captor.getValue().message().contains("48 hours"));
  }
}
