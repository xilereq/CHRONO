package pl.chrono.timemanager.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import pl.chrono.dbmanager.model.Application;
import pl.chrono.dbmanager.model.OvertimeArchive;
import pl.chrono.dbmanager.model.User;
import pl.chrono.dbmanager.model.WorkLog;
import pl.chrono.dbmanager.model.enums.OvertimeType;
import pl.chrono.dbmanager.ports.I_OvertimeData;
import pl.chrono.dbmanager.repository.UserRepository;
import pl.chrono.timemanager.dto.response.OvertimeRecordDto;
import pl.chrono.timemanager.event.LawViolationEvent;
import pl.chrono.timemanager.exception.OvertimeException;
import pl.chrono.timemanager.mapper.OvertimeMapper;

@ExtendWith(MockitoExtension.class)
class OvertimeServiceTest {

  @Mock
  private I_OvertimeData overtimeData;

  @Mock
  private OvertimeMapper overtimeMapper;

  @Mock
  private ApplicationEventPublisher eventPublisher;

  @Mock
  private UserRepository userRepository;

  @InjectMocks
  private OvertimeService overtimeService;

  private Application createApplication(Long id, Integer duration, User employee) {
    Application app = new Application();
    app.setId(id);
    app.setDurationSeconds(duration);
    app.setEmployee(employee);
    return app;
  }

  private WorkLog createWorkLog(User user) {
    WorkLog wl = new WorkLog();
    wl.setUser(user);
    wl.setStartTime(LocalDateTime.now().minusHours(2));
    return wl;
  }

  @Test
  void getOvertimeBalanceTest() {
    when(overtimeData.getUserOvertimeBalance(1)).thenReturn(3600);
    Integer balance = overtimeService.getOvertimeBalance(1);
    assertEquals(3600, balance);
  }

  @Test
  void reserveOvertimeInsufficientBalanceExceptionTest() {
    User user = new User();
    user.setUserId(1);
    Application app = createApplication(10L, 7200, user);

    when(overtimeData.getUserOvertimeBalance(1)).thenReturn(3600);

    assertThrows(
        OvertimeException.InsufficientBalance.class,
        () -> overtimeService.reserveOvertimeForApplication(app)
    );

    verify(overtimeData, never()).saveOvertimeRecord(any());
  }

  @Test
  void reserveOvertimeTest() {
    User user = new User();
    user.setUserId(1);
    Application app = createApplication(10L, 3600, user);

    when(overtimeData.getUserOvertimeBalance(1)).thenReturn(7200);

    overtimeService.reserveOvertimeForApplication(app);

    ArgumentCaptor<OvertimeArchive> captor = ArgumentCaptor.forClass(OvertimeArchive.class);
    verify(overtimeData).saveOvertimeRecord(captor.capture());

    OvertimeArchive saved = captor.getValue();
    assertEquals(OvertimeType.LOCKED, saved.getType());
    assertEquals(3600, saved.getDurationSeconds());
    assertEquals(app, saved.getApplication());
    assertNotNull(saved.getValidUntil());
  }

  @Test
  void reserveOvertimeNullHandleTest() {
    User user = new User();
    user.setUserId(1);
    Application app = createApplication(10L, null, user);

    when(overtimeData.getUserOvertimeBalance(1)).thenReturn(0);

    overtimeService.reserveOvertimeForApplication(app);

    ArgumentCaptor<OvertimeArchive> captor = ArgumentCaptor.forClass(OvertimeArchive.class);
    verify(overtimeData).saveOvertimeRecord(captor.capture());
    assertEquals(0, captor.getValue().getDurationSeconds());
  }

  @Test
  void refundOvertimeTest() {
    User user = new User();
    Application app = createApplication(10L, 3600, user);

    overtimeService.refundOvertimeForApplication(app);

    ArgumentCaptor<OvertimeArchive> captor = ArgumentCaptor.forClass(OvertimeArchive.class);
    verify(overtimeData).saveOvertimeRecord(captor.capture());

    OvertimeArchive saved = captor.getValue();
    assertEquals(OvertimeType.REFUNDED, saved.getType());
    assertEquals(3600, saved.getDurationSeconds());
  }

  @Test
  void refundOvertimeHandleNullTest() {
    User user = new User();
    Application app = createApplication(10L, null, user);

    overtimeService.refundOvertimeForApplication(app);

    ArgumentCaptor<OvertimeArchive> captor = ArgumentCaptor.forClass(OvertimeArchive.class);
    verify(overtimeData).saveOvertimeRecord(captor.capture());
    assertEquals(0, captor.getValue().getDurationSeconds());
  }

  @Test
  void grantOvertimeNoDurationTest() {
    User user = new User();
    WorkLog workLog = createWorkLog(user);

    Application appNull = createApplication(1L, null, user);
    overtimeService.grantOvertime(appNull, workLog);

    Application appZero = createApplication(2L, 0, user);
    overtimeService.grantOvertime(appZero, workLog);

    verify(overtimeData, never()).saveOvertimeRecord(any());
  }

  @Test
  void grantOvertimeTest() {
    User user = new User();
    WorkLog workLog = createWorkLog(user);
    Application app = createApplication(10L, 3600, user);

    overtimeService.grantOvertime(app, workLog);

    ArgumentCaptor<OvertimeArchive> captor = ArgumentCaptor.forClass(OvertimeArchive.class);
    verify(overtimeData).saveOvertimeRecord(captor.capture());

    OvertimeArchive saved = captor.getValue();
    assertEquals(OvertimeType.EARNED, saved.getType());
    assertEquals(3600, saved.getDurationSeconds());
    assertEquals(workLog, saved.getWorkLog());
  }

  @Test
  void registerDebtNoDurationTest() {
    User user = new User();
    WorkLog workLog = createWorkLog(user);

    Application appNull = createApplication(1L, null, user);
    overtimeService.registerDebt(appNull, workLog);

    Application appNegative = createApplication(2L, -10, user);
    overtimeService.registerDebt(appNegative, workLog);

    verify(overtimeData, never()).saveOvertimeRecord(any());
  }

  @Test
  void registerDebtTest() {
    User user = new User();
    WorkLog workLog = createWorkLog(user);
    Application app = createApplication(10L, 3600, user);

    overtimeService.registerDebt(app, workLog);

    ArgumentCaptor<OvertimeArchive> captor = ArgumentCaptor.forClass(OvertimeArchive.class);
    verify(overtimeData).saveOvertimeRecord(captor.capture());

    OvertimeArchive saved = captor.getValue();
    assertEquals(OvertimeType.DEBT, saved.getType());
    assertEquals(3600, saved.getDurationSeconds());
  }

  @Test
  void changeOvertimeStatusLockedToUsedTest() {
    Application app = createApplication(10L, 3600, new User());
    WorkLog workLog = createWorkLog(new User());

    OvertimeArchive lockedArchive = new OvertimeArchive();
    lockedArchive.setType(OvertimeType.LOCKED);

    when(overtimeData.findByApplicationAndType(10L, OvertimeType.LOCKED)).thenReturn(
        Optional.of(lockedArchive));

    overtimeService.changeOvertimeStatus(app, workLog);

    verify(overtimeData).saveOvertimeRecord(lockedArchive);
    assertEquals(OvertimeType.USED, lockedArchive.getType());
    assertEquals(workLog, lockedArchive.getWorkLog());
  }

  @Test
  void changeOvertimeStatusDoNothingTest() {
    Application app = createApplication(10L, 3600, new User());
    WorkLog workLog = createWorkLog(new User());

    when(overtimeData.findByApplicationAndType(10L, OvertimeType.LOCKED)).thenReturn(
        Optional.empty());

    overtimeService.changeOvertimeStatus(app, workLog);

    verify(overtimeData, never()).saveOvertimeRecord(any());
  }

  @Test
  void getOvertimeHistoryTest() {
    OvertimeArchive archive1 = new OvertimeArchive();
    OvertimeArchive archive2 = new OvertimeArchive();

    OvertimeRecordDto dto1 = new OvertimeRecordDto(1L, null, null, null, null);
    OvertimeRecordDto dto2 = new OvertimeRecordDto(2L, null, null, null, null);

    when(overtimeData.findUserOvertimeHistory(1)).thenReturn(List.of(archive1, archive2));
    when(overtimeMapper.toDto(archive1)).thenReturn(dto1);
    when(overtimeMapper.toDto(archive2)).thenReturn(dto2);

    List<OvertimeRecordDto> result = overtimeService.getOvertimeHistory(1);

    assertEquals(2, result.size());
    assertTrue(result.contains(dto1));
    assertTrue(result.contains(dto2));
  }

  @Test
  void grantOvertimeNoPublishEventTest() {
    User user = new User();
    user.setUserId(1);
    WorkLog workLog = createWorkLog(user);
    Application app = createApplication(10L, 3600, user);

    when(overtimeData.sumEarnedOvertimeBetween(any(), any(), any())).thenReturn(36000);

    overtimeService.grantOvertime(app, workLog);

    verify(overtimeData).saveOvertimeRecord(any());
    verify(eventPublisher, never()).publishEvent(any());
  }

  @Test
  void grantOvertimePublishEventTest() {
    User user = new User();
    user.setUserId(1);
    WorkLog workLog = createWorkLog(user);
    Application app = createApplication(10L, 3600, user);

    int limit = 150 * 3600;

    when(overtimeData.sumEarnedOvertimeBetween(any(), any(), any())).thenReturn(limit - 1800);

    overtimeService.grantOvertime(app, workLog);

    ArgumentCaptor<LawViolationEvent> eventCaptor = ArgumentCaptor.forClass(
        LawViolationEvent.class);
    verify(eventPublisher, times(1)).publishEvent(eventCaptor.capture());

    LawViolationEvent firedEvent = eventCaptor.getValue();
    assertEquals(1, firedEvent.userId());
    assertEquals("VIOLATION_YEARLY_OVERTIME", firedEvent.violationType());
    assertEquals(10L, firedEvent.referenceId());
    assertEquals("APPLICATION", firedEvent.referenceType());
    assertTrue(firedEvent.message().contains("150 overtime hours"));
  }

  @Test
  void grantOvertimeLimitAlreadyCrossedTest() {
    User user = new User();
    user.setUserId(1);
    WorkLog workLog = createWorkLog(user);
    Application app = createApplication(10L, 3600, user);

    int limit = 150 * 3600;

    when(overtimeData.sumEarnedOvertimeBetween(any(), any(), any())).thenReturn(limit + 1800);

    overtimeService.grantOvertime(app, workLog);

    verify(overtimeData).saveOvertimeRecord(any());
    verify(eventPublisher, never()).publishEvent(any());
  }

  @Test
  void expireOvertimeNullBalanceTest() {
    when(overtimeData.getExpiringOvertimeBalance(eq(1), any(LocalDateTime.class))).thenReturn(null);

    overtimeService.expireUnusedOvertime(1);

    verify(userRepository, never()).getReferenceById(any());
    verify(overtimeData, never()).saveOvertimeRecord(any());
  }

  @Test
  void expireOvertimeZeroOrNegativeBalanceTest() {
    when(overtimeData.getExpiringOvertimeBalance(eq(1), any(LocalDateTime.class))).thenReturn(0);
    overtimeService.expireUnusedOvertime(1);

    when(overtimeData.getExpiringOvertimeBalance(eq(2), any(LocalDateTime.class))).thenReturn(
        -3600);
    overtimeService.expireUnusedOvertime(2);

    verify(userRepository, never()).getReferenceById(any());
    verify(overtimeData, never()).saveOvertimeRecord(any());
  }

  @Test
  void expireOvertimePositiveBalanceExpireTest() {
    User user = new User();
    user.setUserId(1);

    when(overtimeData.getExpiringOvertimeBalance(eq(1), any(LocalDateTime.class))).thenReturn(7200);
    when(userRepository.getReferenceById(1)).thenReturn(user);

    overtimeService.expireUnusedOvertime(1);

    ArgumentCaptor<OvertimeArchive> captor = ArgumentCaptor.forClass(OvertimeArchive.class);
    verify(overtimeData).saveOvertimeRecord(captor.capture());

    OvertimeArchive saved = captor.getValue();
    assertEquals(user, saved.getUser());
    assertEquals(OvertimeType.EXPIRED, saved.getType());
    assertEquals(7200, saved.getDurationSeconds());
    assertNotNull(saved.getValidUntil());
    assertTrue(saved.getNote().contains("Automatically expired unused overtime for quarter:"));
  }

  @Test
  void revertSettlementForWorkLogDeleteRecordTest() {
    OvertimeArchive earned = new OvertimeArchive();
    earned.setType(OvertimeType.EARNED);

    OvertimeArchive debt = new OvertimeArchive();
    debt.setType(OvertimeType.DEBT);

    when(overtimeData.findByWorkLogId(100)).thenReturn(List.of(earned, debt));

    overtimeService.revertSettlementForWorkLog(100);

    verify(overtimeData).deleteOvertimeRecord(earned);
    verify(overtimeData).deleteOvertimeRecord(debt);
    verify(overtimeData, never()).saveOvertimeRecord(any());
  }

  @Test
  void revertSettlementForWorkLogRevertToLockedTest() {
    OvertimeArchive used = new OvertimeArchive();
    used.setType(OvertimeType.USED);
    used.setWorkLog(new WorkLog());

    when(overtimeData.findByWorkLogId(100)).thenReturn(List.of(used));

    overtimeService.revertSettlementForWorkLog(100);

    verify(overtimeData, never()).deleteOvertimeRecord(any());
    verify(overtimeData).saveOvertimeRecord(used);

    assertEquals(OvertimeType.LOCKED, used.getType());
    assertNull(used.getWorkLog());
    assertEquals("Reverted to LOCKED due to work log modification.", used.getNote());
  }

  @Test
  void revertSettlementForWorkLog_OtherTypes_AreIgnored() {
    OvertimeArchive locked = new OvertimeArchive();
    locked.setType(OvertimeType.LOCKED);

    when(overtimeData.findByWorkLogId(100)).thenReturn(List.of(locked));

    overtimeService.revertSettlementForWorkLog(100);

    verify(overtimeData, never()).deleteOvertimeRecord(any());
    verify(overtimeData, never()).saveOvertimeRecord(any());
  }

  @Test
  void registerPenaltyDebt_ZeroOrNegativeSeconds_ReturnsEarly() {
    WorkLog workLog = new WorkLog();

    overtimeService.registerPenaltyDebt(workLog, 0);
    overtimeService.registerPenaltyDebt(workLog, -3600);

    verify(overtimeData, never()).saveOvertimeRecord(any());
  }

  @Test
  void registerPenaltyDebt_PositiveSeconds_SavesDebt() {
    User user = new User();
    WorkLog workLog = createWorkLog(user);

    overtimeService.registerPenaltyDebt(workLog, 3600);

    ArgumentCaptor<OvertimeArchive> captor = ArgumentCaptor.forClass(OvertimeArchive.class);
    verify(overtimeData).saveOvertimeRecord(captor.capture());

    OvertimeArchive saved = captor.getValue();
    assertEquals(user, saved.getUser());
    assertEquals(OvertimeType.DEBT, saved.getType());
    assertEquals(3600, saved.getDurationSeconds());
    assertNull(saved.getApplication());
    assertEquals(workLog, saved.getWorkLog());
    assertTrue(saved.getNote().contains("Automatic penalty for incomplete workday"));
  }

}
