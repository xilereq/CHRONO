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
import static org.mockito.ArgumentMatchers.any;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
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

  @InjectMocks
  private WorkLogService workLogService;

  @Test
  void startWorkAlreadyStartedExceptionTest() {
    when(timeData.isUserWorking(1)).thenReturn(true);
    assertThrows(WorkException.WorkStarted.class, () -> workLogService.startWork(1));
  }

  @Test
  void startWorkUserNotFoundTest() {
    when(timeData.isUserWorking(1)).thenReturn(false);
    when(userRepository.findById(1)).thenReturn(Optional.empty());
    assertThrows(IllegalArgumentException.class, () -> workLogService.startWork(1));
  }

  @Test
  void startWorkTest() {
    User user = new User();
    WorkLog savedWorkLog = new WorkLog();
    WorkLogDto dto = new WorkLogDto();
    dto.setWorkLogId(1);

    when(timeData.isUserWorking(1)).thenReturn(false);
    when(userRepository.findById(1)).thenReturn(Optional.of(user));
    when(timeData.saveWorkLog(any(WorkLog.class))).thenReturn(savedWorkLog);
    when(workLogMapper.toDto(savedWorkLog)).thenReturn(dto);

    WorkLogDto result = workLogService.startWork(1);

    assertEquals(dto, result);
    verify(timeData).saveWorkLog(any(WorkLog.class));
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
  void endWorkExcessiveWorkTimeExceptionTest() {
    WorkLog workLog = mock(WorkLog.class);
    when(timeData.findActiveWorkLog(1)).thenReturn(Optional.of(workLog));
    when(timeData.isUserOnLeave(1)).thenReturn(false);

    when(workLog.calculateWorkTime(any())).thenReturn(50000L);
    when(workLog.getRequiredSeconds()).thenReturn(28800);
    when(workLog.getRoundingSeconds()).thenReturn(900);
    when(workLog.getMaxAllowedSeconds()).thenReturn(40000);

    assertThrows(WorkException.ExcessiveWorkTime.class, () -> workLogService.endWork(1));

    verify(workLog).forceClose(any());
    verify(timeData).saveWorkLog(workLog);
  }

  @Test
  void endWorkInsufficientWorkTimeExceptionTest() {
    WorkLog workLog = mock(WorkLog.class);
    when(timeData.findActiveWorkLog(1)).thenReturn(Optional.of(workLog));
    when(timeData.isUserOnLeave(1)).thenReturn(false);

    when(workLog.calculateWorkTime(any())).thenReturn(20000L);
    when(workLog.getRequiredSeconds()).thenReturn(28800);
    when(workLog.getRoundingSeconds()).thenReturn(900);
    when(workLog.getMaxAllowedSeconds()).thenReturn(40000);

    assertThrows(WorkException.InsufficientWorkTime.class, () -> workLogService.endWork(1));
  }

  @Test
  void endWorkTest() {
    WorkLog workLog = mock(WorkLog.class);
    WorkLogDto dto = new WorkLogDto();
    dto.setWorkLogId(1);

    when(timeData.findActiveWorkLog(1)).thenReturn(Optional.of(workLog));
    when(timeData.isUserOnLeave(1)).thenReturn(false);

    when(workLog.calculateWorkTime(any())).thenReturn(28000L);
    when(workLog.getRequiredSeconds()).thenReturn(28800);
    when(workLog.getRoundingSeconds()).thenReturn(900);
    when(workLog.getMaxAllowedSeconds()).thenReturn(40000);

    when(timeData.saveWorkLog(workLog)).thenReturn(workLog);
    when(workLogMapper.toDto(workLog)).thenReturn(dto);

    WorkLogDto result = workLogService.endWork(1);

    assertEquals(dto, result);
    verify(workLog).setEndTime(any(LocalDateTime.class));
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

    String result = workLogService.getActiveWorkTime(1);
    assertEquals("01:01:05", result);
  }

  @Test
  void getActiveWorkTimeZeroTest() {
    when(timeData.findActiveWorkLog(1)).thenReturn(Optional.empty());
    assertEquals("00:00:00", workLogService.getActiveWorkTime(1));
  }

  @Test
  void getMonthlyStatsUserNotFoundTest() {
    when(userRepository.findById(1)).thenReturn(Optional.empty());
    assertThrows(IllegalArgumentException.class, () -> workLogService.getMonthlyStats(1));
  }

  @Test
  void getMonthlyStatsWithPolicyTest() {
    User user = mock(User.class);
    Policy policy = mock(Policy.class);

    when(userRepository.findById(1)).thenReturn(Optional.of(user));
    when(user.getActivePolicy()).thenReturn(policy);

    when(policy.getRequiredSecondsForDate(any(LocalDate.class))).thenReturn(28800);

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
  void getTodayTargetWithDifferentApplicationsTest() {
    User user = mock(User.class);
    Policy policy = mock(Policy.class);

    when(userRepository.findById(1)).thenReturn(Optional.of(user));
    when(user.getActivePolicy()).thenReturn(policy);
    when(policy.getRequiredSecondsForDate(any(LocalDate.class))).thenReturn(28800);

    Application app1 = new Application();
    app1.setType(ApplicationType.EARLY_EXIT);
    app1.setDurationSeconds(3600);
    Application app2 = new Application();
    app2.setType(ApplicationType.OVERTIME_TAKE);
    app2.setDurationSeconds(3600);
    Application app3 = new Application();
    app3.setType(ApplicationType.OVERTIME_EARN);
    app3.setDurationSeconds(7200);
    Application app4 = new Application();
    app4.setType(ApplicationType.HOLIDAY_LEAVE);
    app4.setDurationSeconds(null);

    when(applicationRepository.findApprovedByEmployeeAndDate(
        eq(1),
        any(LocalDate.class)
    )).thenReturn(List.of(app1, app2, app3, app4));

    String result = workLogService.getTodayTarget(1);

    assertEquals("08:00:00", result);
  }

  @Test
  void getTodayTargetNegativeTargetTest() {
    User user = mock(User.class);
    when(userRepository.findById(1)).thenReturn(Optional.of(user));
    when(user.getActivePolicy()).thenReturn(null);

    Application app = new Application();
    app.setType(ApplicationType.EARLY_EXIT);
    app.setDurationSeconds(72000);

    when(applicationRepository.findApprovedByEmployeeAndDate(
        eq(1),
        any(LocalDate.class)
    )).thenReturn(List.of(app));

    String result = workLogService.getTodayTarget(1);

    assertEquals("00:00:00", result);
  }

  @Test
  void getTodayTargetUserNotFoundExceptionTest() {
    when(userRepository.findById(1)).thenReturn(Optional.empty());
    assertThrows(IllegalArgumentException.class, () -> workLogService.getTodayTarget(1));
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

      String result = workLogService.getTodayTarget(1);
      assertEquals("00:00:00", result);
    }
  }
}
