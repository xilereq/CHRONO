package pl.chrono.timemanager.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import pl.chrono.dbmanager.model.Policy;
import pl.chrono.dbmanager.model.User;
import pl.chrono.dbmanager.model.WorkLog;
import pl.chrono.dbmanager.model.enums.WorkLocationType;
import pl.chrono.dbmanager.ports.I_TimeData;
import pl.chrono.dbmanager.repository.UserRepository;
import pl.chrono.timemanager.dto.response.WorkLogDto;
import pl.chrono.timemanager.dto.response.WorkStats;
import pl.chrono.timemanager.event.LawViolationEvent;
import pl.chrono.timemanager.exception.UserException;
import pl.chrono.timemanager.exception.WorkException;
import pl.chrono.timemanager.mapper.WorkLogMapper;
import pl.chrono.usermanager.event.UserLoginEvent;

@ExtendWith(MockitoExtension.class)
class WorkLogServiceTest {

  private final Integer USER_ID = 1;
  @Mock
  private I_TimeData timeData;
  @Mock
  private UserRepository userRepository;
  @Mock
  private WorkLogMapper workLogMapper;
  @Mock
  private OvertimeService overtimeService;
  @Mock
  private ApplicationEventPublisher eventPublisher;
  @Mock
  private WorkTimeCalculationService workTimeCalculationService;
  @InjectMocks
  private WorkLogService workLogService;

  @Test
  void getWorkLogsByDateTest() {
    LocalDate date = LocalDate.of(2026, 6, 15);
    WorkLog workLog = new WorkLog();
    WorkLogDto dto = new WorkLogDto();

    when(timeData.findWorkLogsByUserAndDate(
        eq(USER_ID), any(LocalDateTime.class),
        any(LocalDateTime.class)
    )).thenReturn(List.of(workLog));
    when(workLogMapper.toDto(workLog)).thenReturn(dto);

    List<WorkLogDto> result = workLogService.getWorkLogsByDate(USER_ID, date);

    assertThat(result).hasSize(1).contains(dto);
  }

  @Test
  void hasWorkedTodayTest() {
    when(timeData.findWorkLogsByUserAndDate(
        eq(USER_ID), any(LocalDateTime.class),
        any(LocalDateTime.class)
    )).thenReturn(List.of(new WorkLog()));

    boolean result = workLogService.hasUserWorkedToday(USER_ID);

    assertThat(result).isTrue();
  }

  @Test
  void loginThrowsExceptionWhenUserNotFoundTest() {
    UserLoginEvent event = new UserLoginEvent(USER_ID, "test");

    when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

    assertThrows(UserException.NotFound.class, () -> workLogService.handleUserLoginEvent(event));
    verifyNoInteractions(timeData);
  }

  @Test
  void loginReturnsEarlyWhenForcePasswordChangeIsTrueTest() {
    UserLoginEvent event = new UserLoginEvent(USER_ID, "test");
    User mockUser = mock(User.class);

    when(userRepository.findById(USER_ID)).thenReturn(Optional.of(mockUser));
    when(mockUser.isForcePasswordChange()).thenReturn(true);

    workLogService.handleUserLoginEvent(event);

    verifyNoInteractions(timeData);
  }

  @Test
  void loginDoesNotStartWorkWhenAlreadyWorkingTest() {
    UserLoginEvent event = new UserLoginEvent(USER_ID, "test");
    User mockUser = mock(User.class);

    when(userRepository.findById(USER_ID)).thenReturn(Optional.of(mockUser));
    when(mockUser.isForcePasswordChange()).thenReturn(false);

    when(timeData.isUserWorking(USER_ID)).thenReturn(true);

    workLogService.handleUserLoginEvent(event);

    verify(timeData, never()).saveWorkLog(any());
  }

  @Test
  void loginWhileNotWorkingButAfterWorkingDoesNotStartWorkTest() {
    UserLoginEvent event = new UserLoginEvent(USER_ID, "test");
    User mockUser = mock(User.class);

    when(userRepository.findById(USER_ID)).thenReturn(Optional.of(mockUser));
    when(mockUser.isForcePasswordChange()).thenReturn(false);

    when(timeData.isUserWorking(USER_ID)).thenReturn(false);
    when(timeData.findWorkLogsByUserAndDate(eq(USER_ID), any(), any())).thenReturn(
        List.of(new WorkLog()));

    workLogService.handleUserLoginEvent(event);

    verify(timeData, never()).saveWorkLog(any());
  }

  @Test
  void loginAutoStartsWorkTest() {
    UserLoginEvent event = new UserLoginEvent(USER_ID, "test");
    User mockUser = mock(User.class);

    when(userRepository.findById(USER_ID)).thenReturn(Optional.of(mockUser));
    when(userRepository.findByIdWithPessimisticLock(USER_ID)).thenReturn(Optional.of(mockUser));
    when(mockUser.isForcePasswordChange()).thenReturn(false);

    when(timeData.isUserWorking(USER_ID)).thenReturn(false);
    when(timeData.findWorkLogsByUserAndDate(eq(USER_ID), any(), any())).thenReturn(
        Collections.emptyList());

    workLogService.handleUserLoginEvent(event);

    verify(timeData).saveWorkLog(any(WorkLog.class));
  }

  @Test
  void hasNotWorkedTodayTest() {
    when(timeData.findWorkLogsByUserAndDate(
        eq(USER_ID), any(LocalDateTime.class),
        any(LocalDateTime.class)
    )).thenReturn(Collections.emptyList());

    boolean result = workLogService.hasUserWorkedToday(USER_ID);

    assertThat(result).isFalse();
  }

  @Test
  void startWorkWorkStartedExceptionTest() {
    when(timeData.isUserWorking(USER_ID)).thenReturn(true);
    when(userRepository.findByIdWithPessimisticLock(USER_ID)).thenReturn(Optional.of(new User()));

    assertThatThrownBy(
        () -> workLogService.startWork(USER_ID, WorkLocationType.OFFICE)).isInstanceOf(
        WorkException.WorkStarted.class);
  }

  @Test
  void startWorkAlreadyWorkedTodayExceptionTest() {
    when(timeData.isUserWorking(USER_ID)).thenReturn(false);
    when(timeData.findWorkLogsByUserAndDate(eq(USER_ID), any(), any())).thenReturn(
        List.of(new WorkLog()));
    when(userRepository.findByIdWithPessimisticLock(USER_ID)).thenReturn(Optional.of(new User()));

    assertThatThrownBy(
        () -> workLogService.startWork(USER_ID, WorkLocationType.OFFICE)).isInstanceOf(
        WorkException.AlreadyWorkedToday.class);
  }

  @Test
  void startWorkUserNotFoundExceptionTest() {
    assertThatThrownBy(
        () -> workLogService.startWork(USER_ID, WorkLocationType.OFFICE)).isInstanceOf(
        UserException.NotFound.class);
  }

  @Test
  void startWorkAfterEmploymentEndThrowsExceptionTest() {
    User user = new User();
    user.setEmploymentStartDate(LocalDate.now().minusYears(1));
    user.setEmploymentEndDate(LocalDate.now().minusDays(1));

    when(userRepository.findByIdWithPessimisticLock(USER_ID)).thenReturn(Optional.of(user));
    when(timeData.isUserWorking(USER_ID)).thenReturn(false);
    when(timeData.findWorkLogsByUserAndDate(eq(USER_ID), any(), any())).thenReturn(List.of());

    doThrow(new WorkException.OutsideEmploymentPeriod())
        .when(workTimeCalculationService).validateEmploymentPeriod(any(User.class), any(LocalDate.class));

    assertThatThrownBy(() -> workLogService.startWork(USER_ID, WorkLocationType.OFFICE))
        .isInstanceOf(WorkException.OutsideEmploymentPeriod.class);
  }
  @Test
  void startWorkBeforeEmploymentStartThrowsExceptionTest() {
    User user = new User();
    user.setEmploymentStartDate(LocalDate.now().plusDays(1));

    when(userRepository.findByIdWithPessimisticLock(USER_ID)).thenReturn(Optional.of(user));
    when(timeData.isUserWorking(USER_ID)).thenReturn(false);
    when(timeData.findWorkLogsByUserAndDate(eq(USER_ID), any(), any())).thenReturn(List.of());

    doThrow(new WorkException.OutsideEmploymentPeriod())
        .when(workTimeCalculationService).validateEmploymentPeriod(any(User.class), any(LocalDate.class));

    assertThatThrownBy(() -> workLogService.startWork(USER_ID, WorkLocationType.OFFICE))
        .isInstanceOf(WorkException.OutsideEmploymentPeriod.class);
  }

  @Test
  void startWorkOnLastDayOfEmploymentSucceedsTest() {
    User user = new User();
    user.setEmploymentStartDate(LocalDate.now().minusYears(1));
    user.setEmploymentEndDate(LocalDate.now());

    when(userRepository.findByIdWithPessimisticLock(USER_ID)).thenReturn(Optional.of(user));
    when(timeData.isUserWorking(USER_ID)).thenReturn(false);
    when(timeData.findWorkLogsByUserAndDate(eq(USER_ID), any(), any())).thenReturn(List.of());
    when(workTimeCalculationService.hasApprovedHomeOffice(eq(USER_ID), any())).thenReturn(false);
    when(timeData.saveWorkLog(any())).thenReturn(new WorkLog());

    assertThatCode(() -> workLogService.startWork(USER_ID, WorkLocationType.OFFICE))
        .doesNotThrowAnyException();
  }

  @Test
  void startWorkWithNullEmploymentDatesSucceedsTest() {
    User user = new User();
    user.setEmploymentStartDate(null);
    user.setEmploymentEndDate(null);

    when(userRepository.findByIdWithPessimisticLock(USER_ID)).thenReturn(Optional.of(user));
    when(timeData.isUserWorking(USER_ID)).thenReturn(false);
    when(timeData.findWorkLogsByUserAndDate(eq(USER_ID), any(), any())).thenReturn(List.of());
    when(workTimeCalculationService.hasApprovedHomeOffice(eq(USER_ID), any())).thenReturn(false);
    when(timeData.saveWorkLog(any())).thenReturn(new WorkLog());

    assertThatCode(() -> workLogService.startWork(USER_ID, WorkLocationType.OFFICE))
        .doesNotThrowAnyException();
  }

  @Test
  void startWorkWithNoEndDateSucceedsTest() {
    User user = new User();
    user.setEmploymentStartDate(LocalDate.now().minusMonths(6));
    user.setEmploymentEndDate(null);

    when(userRepository.findByIdWithPessimisticLock(USER_ID)).thenReturn(Optional.of(user));
    when(timeData.isUserWorking(USER_ID)).thenReturn(false);
    when(timeData.findWorkLogsByUserAndDate(eq(USER_ID), any(), any())).thenReturn(List.of());
    when(workTimeCalculationService.hasApprovedHomeOffice(eq(USER_ID), any())).thenReturn(false);
    when(timeData.saveWorkLog(any())).thenReturn(new WorkLog());

    assertThatCode(() -> workLogService.startWork(USER_ID, WorkLocationType.OFFICE))
        .doesNotThrowAnyException();
  }

  @Test
  void startWorkNoHomeOfficeApplicationExceptionTest() {
    when(timeData.isUserWorking(USER_ID)).thenReturn(false);
    when(timeData.findWorkLogsByUserAndDate(eq(USER_ID), any(), any())).thenReturn(List.of());
    when(userRepository.findByIdWithPessimisticLock(USER_ID)).thenReturn(Optional.of(new User()));

    when(workTimeCalculationService.hasApprovedHomeOffice(eq(USER_ID), any())).thenReturn(false);

    assertThatThrownBy(
        () -> workLogService.startWork(USER_ID, WorkLocationType.HOME_OFFICE)).isInstanceOf(
        WorkException.NoHomeOfficeApplication.class);
  }

  @Test
  void startWorkHomeOfficeLocationWithApplicationIfNotSpecifiedTest() {
    when(timeData.isUserWorking(USER_ID)).thenReturn(false);
    when(timeData.findWorkLogsByUserAndDate(eq(USER_ID), any(), any())).thenReturn(List.of());
    when(userRepository.findByIdWithPessimisticLock(USER_ID)).thenReturn(Optional.of(new User()));

    when(workTimeCalculationService.hasApprovedHomeOffice(eq(USER_ID), any())).thenReturn(true);

    WorkLog savedLog = new WorkLog();
    when(timeData.saveWorkLog(any())).thenReturn(savedLog);

    workLogService.startWork(USER_ID, null);

    ArgumentCaptor<WorkLog> logCaptor = ArgumentCaptor.forClass(WorkLog.class);
    verify(timeData).saveWorkLog(logCaptor.capture());

    assertThat(logCaptor.getValue().getLocationType()).isEqualTo(WorkLocationType.HOME_OFFICE);
  }

  @Test
  void startWorkOfficeLocationWithoutApplicationIfNotSpecifiedTest() {
    when(timeData.isUserWorking(USER_ID)).thenReturn(false);
    when(timeData.findWorkLogsByUserAndDate(eq(USER_ID), any(), any())).thenReturn(List.of());
    when(userRepository.findByIdWithPessimisticLock(USER_ID)).thenReturn(Optional.of(new User()));

    when(workTimeCalculationService.hasApprovedHomeOffice(eq(USER_ID), any())).thenReturn(false);

    WorkLog savedLog = new WorkLog();
    when(timeData.saveWorkLog(any())).thenReturn(savedLog);

    workLogService.startWork(USER_ID, null);

    ArgumentCaptor<WorkLog> logCaptor = ArgumentCaptor.forClass(WorkLog.class);
    verify(timeData).saveWorkLog(logCaptor.capture());

    assertThat(logCaptor.getValue().getLocationType()).isEqualTo(WorkLocationType.OFFICE);
  }

  @Test
  void startWorkWithHOApplicationPassesTest() {
    when(timeData.isUserWorking(USER_ID)).thenReturn(false);
    when(timeData.findWorkLogsByUserAndDate(eq(USER_ID), any(), any())).thenReturn(List.of());
    when(userRepository.findByIdWithPessimisticLock(USER_ID)).thenReturn(Optional.of(new User()));

    when(workTimeCalculationService.hasApprovedHomeOffice(eq(USER_ID), any())).thenReturn(true);
    when(timeData.saveWorkLog(any())).thenReturn(new WorkLog());

    assertThatCode(() -> workLogService.startWork(
        USER_ID,
        WorkLocationType.HOME_OFFICE
    )).doesNotThrowAnyException();
  }

  @Test
  void startWorkWithHOOfficeApplicationButChoseOfficeTest() {
    when(timeData.isUserWorking(USER_ID)).thenReturn(false);
    when(timeData.findWorkLogsByUserAndDate(eq(USER_ID), any(), any())).thenReturn(List.of());
    when(userRepository.findByIdWithPessimisticLock(USER_ID)).thenReturn(Optional.of(new User()));

    when(workTimeCalculationService.hasApprovedHomeOffice(eq(USER_ID), any())).thenReturn(false);
    when(timeData.saveWorkLog(any())).thenReturn(new WorkLog());

    assertThatCode(() -> workLogService.startWork(
        USER_ID,
        WorkLocationType.OFFICE
    )).doesNotThrowAnyException();
  }

  @Test
  void cancelWorkNoActiveWorkExceptionTest() {
    when(timeData.findActiveWorkLog(USER_ID)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> workLogService.cancelWork(USER_ID)).isInstanceOf(
        WorkException.NoActiveWork.class);
  }

  @Test
  void cancelWorkExpiredCancellationExceptionTest() {
    WorkLog workLog = new WorkLog();
    workLog.setStartTime(LocalDateTime.now().minusMinutes(20));

    when(timeData.findActiveWorkLog(USER_ID)).thenReturn(Optional.of(workLog));

    assertThatThrownBy(() -> workLogService.cancelWork(USER_ID)).isInstanceOf(
        WorkException.CancellationExpired.class);
  }

  @Test
  void cancelWorkProperlyDeletesLogTest() {
    WorkLog workLog = new WorkLog();

    workLog.setStartTime(LocalDateTime.now().minusSeconds(60));

    when(timeData.findActiveWorkLog(USER_ID)).thenReturn(Optional.of(workLog));
    assertThatCode(() -> workLogService.cancelWork(USER_ID)).doesNotThrowAnyException();

    verify(timeData).deleteWorkLog(workLog);
  }

  @Test
  void endWorkNoActiveWorkExceptionTest() {
    when(timeData.findActiveWorkLog(USER_ID)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> workLogService.endWork(USER_ID)).isInstanceOf(
        WorkException.NoActiveWork.class);
  }

  @Test
  void endWorkUserOnLeaveExceptionTest() {
    WorkLog workLog = new WorkLog();
    when(timeData.findActiveWorkLog(USER_ID)).thenReturn(Optional.of(workLog));
    when(timeData.isUserOnLeave(USER_ID)).thenReturn(true);

    assertThatThrownBy(() -> workLogService.endWork(USER_ID)).isInstanceOf(
        WorkException.LeaveStarted.class);
  }

  @Test
  void endWorkInsufficientWorkTimeExceptionTest() {
    WorkLog spyLog = spy(new WorkLog());
    spyLog.setStartTime(LocalDateTime.now().minusHours(4));
    User user = new User();
    user.setUserId(USER_ID);
    spyLog.setUser(user);

    when(timeData.findActiveWorkLog(USER_ID)).thenReturn(Optional.of(spyLog));
    when(timeData.isUserOnLeave(USER_ID)).thenReturn(false);

    doReturn(14400L).when(spyLog).calculateWorkTime(any());

    when(workTimeCalculationService.getAdjustedRequiredSeconds(
        eq(USER_ID), any(),
        any()
    )).thenReturn(8 * 3600);
    when(workTimeCalculationService.getRoundingSeconds(any())).thenReturn(15 * 60);
    when(workTimeCalculationService.getMaxAllowedSeconds(any())).thenReturn(12 * 3600);
    when(workTimeCalculationService.formatSecondsToTime(any(Long.class))).thenReturn("04:00:00");

    assertThatThrownBy(() -> workLogService.endWork(USER_ID)).isInstanceOf(
        WorkException.InsufficientWorkTime.class).hasMessageContaining("04:00:00");
  }

  @Test
  void endWorkMaxTimeExceededTodayEventTest() {
    WorkLog spyLog = spy(new WorkLog());
    spyLog.setWorkLogId(99);
    spyLog.setStartTime(LocalDateTime.now().minusHours(14));
    User user = new User();
    user.setUserId(USER_ID);
    spyLog.setUser(user);

    when(timeData.findActiveWorkLog(USER_ID)).thenReturn(Optional.of(spyLog));
    when(timeData.isUserOnLeave(USER_ID)).thenReturn(false);

    doReturn(14 * 3600L).when(spyLog).calculateWorkTime(any());

    when(workTimeCalculationService.getAdjustedRequiredSeconds(
        eq(USER_ID), any(),
        any()
    )).thenReturn(8 * 3600);
    when(workTimeCalculationService.getRoundingSeconds(any())).thenReturn(15 * 60);
    when(workTimeCalculationService.getMaxAllowedSeconds(any())).thenReturn(12 * 3600);

    WorkLogDto mockDto = new WorkLogDto();
    when(timeData.saveWorkLog(any())).thenReturn(spyLog);
    when(workLogMapper.toDto(any())).thenReturn(mockDto);

    workLogService.endWork(USER_ID);

    verify(spyLog).setEndTime(any(LocalDateTime.class));

    ArgumentCaptor<LawViolationEvent> eventCaptor = ArgumentCaptor.forClass(
        LawViolationEvent.class);
    verify(eventPublisher).publishEvent(eventCaptor.capture());
    assertThat(eventCaptor.getValue().violationType()).isEqualTo("VIOLATION_MAX_WORK");
  }

  @Test
  void endWorkRestViolationEventTest() {
    WorkLog spyLog = spy(new WorkLog());
    spyLog.setWorkLogId(100);
    spyLog.setStartTime(LocalDateTime.now().minusHours(8));
    User user = new User();
    user.setUserId(USER_ID);
    spyLog.setUser(user);

    when(timeData.findActiveWorkLog(USER_ID)).thenReturn(Optional.of(spyLog));
    when(timeData.isUserOnLeave(USER_ID)).thenReturn(false);

    doReturn(8 * 3600L).when(spyLog).calculateWorkTime(any());

    when(workTimeCalculationService.getAdjustedRequiredSeconds(
        eq(USER_ID), any(),
        any()
    )).thenReturn(8 * 3600);
    when(workTimeCalculationService.getRoundingSeconds(any())).thenReturn(15 * 60);
    when(workTimeCalculationService.getMaxAllowedSeconds(any())).thenReturn(12 * 3600);
    when(timeData.saveWorkLog(any())).thenReturn(spyLog);

    doNothing().when(spyLog).setEndTime(any());

    WorkLog lastLog = new WorkLog();
    lastLog.setWorkLogId(99);
    lastLog.setEndTime(spyLog.getStartTime().minusHours(5));
    when(timeData.findLastClosedWorkLog(USER_ID)).thenReturn(Optional.of(lastLog));

    workLogService.endWork(USER_ID);

    ArgumentCaptor<LawViolationEvent> eventCaptor = ArgumentCaptor.forClass(
        LawViolationEvent.class);
    verify(eventPublisher).publishEvent(eventCaptor.capture());
    assertThat(eventCaptor.getValue().violationType()).isEqualTo("VIOLATION_REST");
  }

  @Test
  void endWorkWeeklyLimitEventTest() {
    WorkLog spyCurrentLog = spy(new WorkLog());
    spyCurrentLog.setWorkLogId(100);
    spyCurrentLog.setStartTime(LocalDateTime.now().minusHours(8));

    User spyUser = spy(new User());
    spyUser.setUserId(USER_ID);

    Policy policy = new Policy();
    policy.setMaxWeeklyHours(40);

    doReturn(policy).when(spyUser).getPolicyForDate(any());

    spyCurrentLog.setUser(spyUser);

    when(timeData.findActiveWorkLog(USER_ID)).thenReturn(Optional.of(spyCurrentLog));
    when(timeData.isUserOnLeave(USER_ID)).thenReturn(false);

    doReturn(8 * 3600L).when(spyCurrentLog).calculateWorkTime(any());
    when(workTimeCalculationService.getAdjustedRequiredSeconds(
        eq(USER_ID), any(),
        any()
    )).thenReturn(8 * 3600);
    when(workTimeCalculationService.getRoundingSeconds(any())).thenReturn(0);
    when(workTimeCalculationService.getMaxAllowedSeconds(any())).thenReturn(12 * 3600);
    when(timeData.saveWorkLog(any())).thenReturn(spyCurrentLog);

    WorkLog spyPastLog = spy(new WorkLog());
    spyPastLog.setStartTime(LocalDateTime.now().minusDays(2));
    spyPastLog.setUser(spyUser);

    doReturn(35 * 3600L).when(spyPastLog)
        .getEffectiveWorkTime(any(), any(Integer.class), any(Integer.class));
    doReturn(8 * 3600L).when(spyCurrentLog)
        .getEffectiveWorkTime(any(), any(Integer.class), any(Integer.class));

    when(timeData.findUserWorkLogsAfter(eq(USER_ID), any())).thenReturn(
        List.of(spyPastLog, spyCurrentLog));

    workLogService.endWork(USER_ID);

    ArgumentCaptor<LawViolationEvent> eventCaptor = ArgumentCaptor.forClass(
        LawViolationEvent.class);
    verify(eventPublisher).publishEvent(eventCaptor.capture());
    assertThat(eventCaptor.getValue().violationType()).isEqualTo("VIOLATION_MAX_WEEKLY_WORK");
  }

  @Test
  void endWorkNotPublishRestViolationEventTest() {
    WorkLog spyLog = spy(new WorkLog());
    spyLog.setWorkLogId(100);
    User user = new User();
    user.setUserId(USER_ID);
    spyLog.setUser(user);
    spyLog.setStartTime(LocalDateTime.now().minusHours(8));

    when(timeData.findActiveWorkLog(USER_ID)).thenReturn(Optional.of(spyLog));
    when(timeData.isUserOnLeave(USER_ID)).thenReturn(false);
    doReturn(8 * 3600L).when(spyLog).calculateWorkTime(any());

    when(workTimeCalculationService.getAdjustedRequiredSeconds(any(), any(), any())).thenReturn(
        8 * 3600);
    when(workTimeCalculationService.getRoundingSeconds(any())).thenReturn(0);
    when(workTimeCalculationService.getMaxAllowedSeconds(any())).thenReturn(12 * 3600);
    when(timeData.saveWorkLog(any())).thenReturn(spyLog);

    WorkLog sameLog = new WorkLog();
    sameLog.setWorkLogId(100);
    sameLog.setStartTime(LocalDateTime.now().minusHours(8));
    when(timeData.findLastClosedWorkLog(USER_ID)).thenReturn(Optional.of(sameLog));

    workLogService.endWork(USER_ID);

    verify(eventPublisher, never()).publishEvent(any(LawViolationEvent.class));
  }

  @Test
  void endWorkProperlyIgnoresEventInsufficientWorkEventTest() {
    WorkLog spyLog = spy(new WorkLog());
    spyLog.setWorkLogId(100);
    spyLog.setStartTime(LocalDateTime.now().minusHours(8));
    User user = new User();
    user.setUserId(USER_ID);
    spyLog.setUser(user);

    when(timeData.findActiveWorkLog(USER_ID)).thenReturn(Optional.of(spyLog));
    when(timeData.isUserOnLeave(USER_ID)).thenReturn(false);
    doReturn(8 * 3600L).when(spyLog).calculateWorkTime(any());

    when(workTimeCalculationService.getAdjustedRequiredSeconds(any(), any(), any())).thenReturn(
        8 * 3600);
    when(workTimeCalculationService.getRoundingSeconds(any())).thenReturn(0);
    when(workTimeCalculationService.getMaxAllowedSeconds(any())).thenReturn(12 * 3600);
    when(timeData.saveWorkLog(any())).thenReturn(spyLog);

    WorkLog lastLog = new WorkLog();
    lastLog.setWorkLogId(99);
    lastLog.setStartTime(LocalDateTime.now().minusHours(24));
    lastLog.setEndTime(spyLog.getStartTime().minusHours(12));
    when(timeData.findLastClosedWorkLog(USER_ID)).thenReturn(Optional.of(lastLog));

    workLogService.endWork(USER_ID);

    verify(eventPublisher, never()).publishEvent(any(LawViolationEvent.class));
  }

  @Test
  void getActiveWorkTimeWhileWorkingTest() {
    WorkLog spyLog = spy(new WorkLog());
    when(timeData.findActiveWorkLog(USER_ID)).thenReturn(Optional.of(spyLog));
    doReturn(3600L).when(spyLog).calculateWorkTime(any());
    when(workTimeCalculationService.formatSecondsToTime(3600L)).thenReturn("01:00:00");

    String result = workLogService.getActiveWorkTime(USER_ID);

    assertThat(result).isEqualTo("01:00:00");
  }

  @Test
  void endWorkNullStartTimeTest() {
    WorkLog spyLog = org.mockito.Mockito.spy(new WorkLog());
    spyLog.setWorkLogId(100);
    spyLog.setStartTime(null);

    User spyUser = org.mockito.Mockito.spy(new User());
    spyUser.setUserId(USER_ID);
    spyLog.setUser(spyUser);

    when(timeData.findActiveWorkLog(USER_ID)).thenReturn(Optional.of(spyLog));
    when(timeData.isUserOnLeave(USER_ID)).thenReturn(false);

    doReturn(1000L).when(spyLog).calculateWorkTime(any());

    when(workTimeCalculationService.getAdjustedRequiredSeconds(any(), any(), any())).thenReturn(
        8 * 3600);
    when(workTimeCalculationService.getRoundingSeconds(any())).thenReturn(0);
    when(workTimeCalculationService.getMaxAllowedSeconds(any())).thenReturn(12 * 3600);
    when(workTimeCalculationService.formatSecondsToTime(any(Long.class))).thenReturn("00:16:40");

    assertThatThrownBy(() -> workLogService.endWork(USER_ID)).isInstanceOf(
        WorkException.InsufficientWorkTime.class);

    verify(spyUser).getPolicyForDate(any(LocalDate.class));
  }

  @Test
  void getActiveWorkTimeWhileNotWorkingTest() {
    when(timeData.findActiveWorkLog(USER_ID)).thenReturn(Optional.empty());

    String result = workLogService.getActiveWorkTime(USER_ID);

    assertThat(result).isEqualTo("00:00:00");
  }

  @Test
  void todayTargetUserNotFoundTest() {
    when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> workLogService.getTodayTarget(USER_ID)).isInstanceOf(
        UserException.NotFound.class);
  }

  @Test
  void getTodayTargetTest() {
    User user = new User();
    when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
    when(workTimeCalculationService.getAdjustedRequiredSeconds(
        eq(USER_ID), any(),
        any()
    )).thenReturn(28800);
    when(workTimeCalculationService.formatSecondsToTime(28800)).thenReturn("08:00:00");

    String result = workLogService.getTodayTarget(USER_ID);

    assertThat(result).isEqualTo("08:00:00");
  }

  @Test
  void getMonthlyStatsUserNotFoundTest() {
    when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> workLogService.getMonthlyStats(USER_ID)).isInstanceOf(
        UserException.NotFound.class);
  }

  @Test
  void getMonthlyStatsProperTest() {
    User user = new User();
    user.setUserId(USER_ID);
    when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));

    when(workTimeCalculationService.isWithinEmploymentPeriod(any(User.class), any(LocalDate.class)))
        .thenReturn(true);
    when(workTimeCalculationService.getBaseRequiredSeconds(any(), any())).thenReturn(8 * 3600);
    when(overtimeService.getOvertimeBalance(USER_ID)).thenReturn(3600);

    WorkLog spyLog = spy(new WorkLog());
    spyLog.setStartTime(LocalDate.now().withDayOfMonth(1).atStartOfDay());
    spyLog.setEndTime(spyLog.getStartTime().plusHours(8));
    spyLog.setUser(user);
    doReturn(8 * 3600L).when(spyLog)
        .getEffectiveWorkTime(any(), any(Integer.class), any(Integer.class));

    when(timeData.findUserWorkLogsAfter(eq(USER_ID), any())).thenReturn(List.of(spyLog));

    WorkStats stats = workLogService.getMonthlyStats(USER_ID);

    assertThat(stats).isNotNull();
    assertThat(stats.totalWorkTime()).isEqualTo(8);
    assertThat(stats.overtimeSeconds()).isEqualTo(3600);
    assertThat(stats.totalMonthlyHours()).isGreaterThan(0);
  }

  @Test
  void getMonthlyStatsFiltersOutNullEndTimesAndFutureLogsTest() {
    User user = new User();
    user.setUserId(USER_ID);
    when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
    when(workTimeCalculationService.isWithinEmploymentPeriod(any(User.class), any(LocalDate.class)))
        .thenReturn(true);
    when(workTimeCalculationService.getBaseRequiredSeconds(any(), any())).thenReturn(8 * 3600);

    WorkLog openLog = new WorkLog();
    openLog.setStartTime(LocalDate.now().withDayOfMonth(1).atStartOfDay());
    openLog.setEndTime(null);

    WorkLog futureLog = new WorkLog();
    futureLog.setStartTime(LocalDate.now().atStartOfDay());
    futureLog.setEndTime(LocalDate.now().atStartOfDay().plusHours(4));

    when(timeData.findUserWorkLogsAfter(eq(USER_ID), any())).thenReturn(
        List.of(openLog, futureLog));

    WorkStats stats = workLogService.getMonthlyStats(USER_ID);

    assertThat(stats.totalWorkTime()).isZero();
  }

  @Test
  void endWorkDefault48HoursWeeklyLimitTest() {
    WorkLog spyCurrentLog = spy(new WorkLog());
    spyCurrentLog.setWorkLogId(100);
    spyCurrentLog.setStartTime(LocalDateTime.now().minusHours(8));
    User spyUser = spy(new User());
    spyUser.setUserId(USER_ID);

    Policy policy = new Policy();
    policy.setMaxWeeklyHours(null);
    doReturn(policy).when(spyUser).getPolicyForDate(any());
    spyCurrentLog.setUser(spyUser);

    when(timeData.findActiveWorkLog(USER_ID)).thenReturn(Optional.of(spyCurrentLog));
    when(timeData.isUserOnLeave(USER_ID)).thenReturn(false);
    when(timeData.saveWorkLog(any())).thenReturn(spyCurrentLog);

    doReturn(8 * 3600L).when(spyCurrentLog).calculateWorkTime(any());
    when(workTimeCalculationService.getAdjustedRequiredSeconds(any(), any(), any())).thenReturn(
        8 * 3600);
    when(workTimeCalculationService.getRoundingSeconds(any())).thenReturn(0);
    when(workTimeCalculationService.getMaxAllowedSeconds(any())).thenReturn(12 * 3600);

    WorkLog spyPastLog = spy(new WorkLog());
    spyPastLog.setUser(spyUser);
    spyPastLog.setStartTime(LocalDateTime.now().minusDays(2));

    doReturn(45 * 3600L).when(spyPastLog)
        .getEffectiveWorkTime(any(), any(Integer.class), any(Integer.class));
    doReturn(5 * 3600L).when(spyCurrentLog)
        .getEffectiveWorkTime(any(), any(Integer.class), any(Integer.class));
    when(timeData.findUserWorkLogsAfter(eq(USER_ID), any())).thenReturn(
        List.of(spyPastLog, spyCurrentLog));

    workLogService.endWork(USER_ID);

    ArgumentCaptor<LawViolationEvent> captor = ArgumentCaptor.forClass(LawViolationEvent.class);
    verify(eventPublisher).publishEvent(captor.capture());
    assertThat(captor.getValue().message()).contains("48 godzin");
  }

  @Test
  void endWorkNoDuplicateWeeklyLimitEventTest() {
    WorkLog spyCurrentLog = spy(new WorkLog());
    spyCurrentLog.setWorkLogId(100);
    spyCurrentLog.setStartTime(LocalDateTime.now().minusHours(8));
    User spyUser = spy(new User());
    spyUser.setUserId(USER_ID);

    Policy policy = new Policy();
    policy.setMaxWeeklyHours(40);
    doReturn(policy).when(spyUser).getPolicyForDate(any());
    spyCurrentLog.setUser(spyUser);

    when(timeData.findActiveWorkLog(USER_ID)).thenReturn(Optional.of(spyCurrentLog));
    when(timeData.isUserOnLeave(USER_ID)).thenReturn(false);
    when(timeData.saveWorkLog(any())).thenReturn(spyCurrentLog);

    doReturn(8 * 3600L).when(spyCurrentLog).calculateWorkTime(any());
    when(workTimeCalculationService.getAdjustedRequiredSeconds(any(), any(), any())).thenReturn(
        8 * 3600);
    when(workTimeCalculationService.getRoundingSeconds(any())).thenReturn(0);
    when(workTimeCalculationService.getMaxAllowedSeconds(any())).thenReturn(12 * 3600);

    WorkLog spyPastLog = spy(new WorkLog());
    spyPastLog.setUser(spyUser);
    spyPastLog.setStartTime(LocalDateTime.now().minusDays(2));

    doReturn(42 * 3600L).when(spyPastLog)
        .getEffectiveWorkTime(any(), any(Integer.class), any(Integer.class));
    doReturn(5 * 3600L).when(spyCurrentLog)
        .getEffectiveWorkTime(any(), any(Integer.class), any(Integer.class));
    when(timeData.findUserWorkLogsAfter(eq(USER_ID), any())).thenReturn(
        List.of(spyPastLog, spyCurrentLog));

    workLogService.endWork(USER_ID);

    verify(eventPublisher, never()).publishEvent(any(LawViolationEvent.class));
  }

  @Test
  void getMonthlyStatsExcludesDaysOutsideEmploymentPeriodTest() {
    User user = new User();
    user.setUserId(USER_ID);
    when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));

    when(workTimeCalculationService.isWithinEmploymentPeriod(any(User.class), any(LocalDate.class)))
        .thenReturn(false);
    when(overtimeService.getOvertimeBalance(USER_ID)).thenReturn(0);
    when(timeData.findUserWorkLogsAfter(eq(USER_ID), any())).thenReturn(List.of());

    WorkStats stats = workLogService.getMonthlyStats(USER_ID);

    assertThat(stats.totalMonthlyHours()).isZero();
    verify(workTimeCalculationService, never()).getBaseRequiredSeconds(any(), any());
    verify(workTimeCalculationService, never()).getExcusedSecondsForDate(any(), any(), any());
  }
}
