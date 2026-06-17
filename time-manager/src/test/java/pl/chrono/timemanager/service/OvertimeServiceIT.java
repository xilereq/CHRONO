package pl.chrono.timemanager.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import pl.chrono.dbmanager.model.Application;
import pl.chrono.dbmanager.model.OvertimeArchive;
import pl.chrono.dbmanager.model.User;
import pl.chrono.dbmanager.model.WorkLog;
import pl.chrono.dbmanager.model.enums.ApplicationType;
import pl.chrono.dbmanager.model.enums.OvertimeType;
import pl.chrono.dbmanager.repository.ApplicationRepository;
import pl.chrono.dbmanager.repository.OvertimeArchiveRepository;
import pl.chrono.dbmanager.repository.UserRepository;
import pl.chrono.dbmanager.repository.WorkLogRepository;
import pl.chrono.timemanager.TestConfig;
import pl.chrono.timemanager.dto.response.OvertimeRecordDto;
import pl.chrono.timemanager.exception.OvertimeException;


@SpringBootTest(classes = TestConfig.class)
@Transactional
class OvertimeServiceIT {

  @Autowired
  private OvertimeService overtimeService;

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private ApplicationRepository applicationRepository;

  @Autowired
  private WorkLogRepository workLogRepository;

  @Autowired
  private OvertimeArchiveRepository overtimeArchiveRepository;

  private User user;

  @BeforeEach
  void setUp() {
    user = userRepository.save(buildUser("Jan", "Kowalski", "jan.kowalski@test.com"));
  }

  @Test
  void getOvertimeBalance_sumsEarnedMinusUsedTypes() {
    persistArchive(OvertimeType.EARNED, 5 * 3600, null, null);
    persistArchive(OvertimeType.USED, 2 * 3600, null, null);

    Integer balance = overtimeService.getOvertimeBalance(user.getUserId());

    assertThat(balance).isEqualTo(3 * 3600);
  }

  @Test
  void getOvertimeBalance_zeroWhenNoArchives() {
    Integer balance = overtimeService.getOvertimeBalance(user.getUserId());

    assertThat(balance).isEqualTo(0);
  }

  @Test
  void reserveOvertimeForApplication_throwsInsufficientBalance_whenBalanceTooLow() {
    Application application = persistApplication(ApplicationType.OVERTIME_TAKE, 3600);

    assertThatThrownBy(
        () -> overtimeService.reserveOvertimeForApplication(application)).isInstanceOf(
        OvertimeException.InsufficientBalance.class);
  }

  @Test
  void reserveOvertimeForApplication_createsLockedArchive_whenBalanceSufficient() {
    persistArchive(OvertimeType.EARNED, 4 * 3600, null, null);
    Application application = persistApplication(ApplicationType.OVERTIME_TAKE, 3600);

    overtimeService.reserveOvertimeForApplication(application);

    Integer balanceAfter = overtimeService.getOvertimeBalance(user.getUserId());
    assertThat(balanceAfter).isEqualTo(4 * 3600 - 3600);
    var lockedArchive = overtimeArchiveRepository.findByApplication_IdAndType(
        application.getId(), OvertimeType.LOCKED);
    assertThat(lockedArchive).isPresent();
    assertThat(lockedArchive.get().getDurationSeconds()).isEqualTo(3600);
  }

  @Test
  void refundOvertimeForApplication_createsRefundedArchive() {
    Application application = persistApplication(ApplicationType.OVERTIME_TAKE, 1800);

    overtimeService.refundOvertimeForApplication(application);

    Integer balance = overtimeService.getOvertimeBalance(user.getUserId());
    assertThat(balance).isEqualTo(1800);
  }

  @Test
  void grantOvertime_doesNothing_whenDurationIsZeroOrNull() {
    Application application = persistApplication(ApplicationType.OVERTIME_EARN, 0);
    WorkLog workLog = persistWorkLog(LocalDateTime.now().minusHours(1));

    overtimeService.grantOvertime(application, workLog);

    assertThat(overtimeArchiveRepository.findByUser_UserId(user.getUserId())).isEmpty();
  }

  @Test
  void grantOvertime_createsEarnedArchive_whenDurationPositive() {
    Application application = persistApplication(ApplicationType.OVERTIME_EARN, 7200);
    WorkLog workLog = persistWorkLog(LocalDateTime.now().minusHours(2));

    overtimeService.grantOvertime(application, workLog);

    Integer balance = overtimeService.getOvertimeBalance(user.getUserId());
    assertThat(balance).isEqualTo(7200);
    List<OvertimeArchive> archives = overtimeArchiveRepository.findByUser_UserId(user.getUserId());
    assertThat(archives).hasSize(1);
    assertThat(archives.get(0).getType()).isEqualTo(OvertimeType.EARNED);
  }

  @Test
  void registerDebt_doesNothing_whenDurationIsZero() {
    Application application = persistApplication(ApplicationType.OVERTIME_TAKE, 0);
    WorkLog workLog = persistWorkLog(LocalDateTime.now().minusHours(1));

    overtimeService.registerDebt(application, workLog);

    assertThat(overtimeArchiveRepository.findByUser_UserId(user.getUserId())).isEmpty();
  }

  @Test
  void registerDebt_createsDebtArchive_whenDurationPositive() {
    Application application = persistApplication(ApplicationType.OVERTIME_TAKE, 1800);
    WorkLog workLog = persistWorkLog(LocalDateTime.now().minusHours(1));

    overtimeService.registerDebt(application, workLog);

    Integer balance = overtimeService.getOvertimeBalance(user.getUserId());
    assertThat(balance).isEqualTo(-1800);
  }

  @Test
  void changeOvertimeStatus_movesLockedToUsed_whenMatchingReservationExists() {
    Application application = persistApplication(ApplicationType.OVERTIME_TAKE, 3600);
    persistArchive(OvertimeType.EARNED, 7200, null, null);
    overtimeService.reserveOvertimeForApplication(application);
    WorkLog workLog = persistWorkLog(LocalDateTime.now().minusHours(1));

    overtimeService.changeOvertimeStatus(application, workLog);

    OvertimeArchive archive = overtimeArchiveRepository.findByApplication_IdAndType(
        application.getId(), OvertimeType.USED).orElseThrow();
    assertThat(archive.getWorkLog().getWorkLogId()).isEqualTo(workLog.getWorkLogId());
  }

  @Test
  void changeOvertimeStatus_doesNothing_whenNoMatchingLockedReservation() {
    Application application = persistApplication(ApplicationType.OVERTIME_TAKE, 3600);
    WorkLog workLog = persistWorkLog(LocalDateTime.now().minusHours(1));

    overtimeService.changeOvertimeStatus(application, workLog);

    assertThat(overtimeArchiveRepository.findByUser_UserId(user.getUserId())).isEmpty();
  }

  @Test
  void expireUnusedOvertime_doesNothing_whenNoExpiringBalance() {
    overtimeService.expireUnusedOvertime(user.getUserId());

    assertThat(overtimeArchiveRepository.findByUser_UserId(user.getUserId())).isEmpty();
  }

  @Test
  void expireUnusedOvertime_zerosOutBalance_whenEarnedOvertimeHasExpired() {
    persistArchive(OvertimeType.EARNED, 3600, null, LocalDateTime.now().minusDays(1));

    overtimeService.expireUnusedOvertime(user.getUserId());

    Integer balance = overtimeService.getOvertimeBalance(user.getUserId());
    assertThat(balance).isEqualTo(0);
  }

  @Test
  void getOvertimeHistory_returnsAllRecordsForUser() {
    persistArchive(OvertimeType.EARNED, 3600, null, null);
    persistArchive(OvertimeType.USED, 1800, null, null);

    List<OvertimeRecordDto> history = overtimeService.getOvertimeHistory(user.getUserId());

    assertThat(history).hasSize(2);
    assertThat(history).extracting(OvertimeRecordDto::type)
        .containsExactlyInAnyOrder("EARNED", "USED");
  }

  @Test
  void revertSettlementForWorkLog_deletesEarnedAndDebtRecords() {
    WorkLog workLog = persistWorkLog(LocalDateTime.now().minusHours(3));
    persistArchive(OvertimeType.EARNED, 3600, workLog, null);
    persistArchive(OvertimeType.DEBT, 600, workLog, null);

    overtimeService.revertSettlementForWorkLog(workLog.getWorkLogId());

    assertThat(overtimeArchiveRepository.findByWorkLog_WorkLogId(workLog.getWorkLogId())).isEmpty();
  }

  @Test
  void revertSettlementForWorkLog_revertsUsedRecordsBackToLocked() {
    WorkLog workLog = persistWorkLog(LocalDateTime.now().minusHours(3));
    persistArchive(OvertimeType.USED, 3600, workLog, null);

    overtimeService.revertSettlementForWorkLog(workLog.getWorkLogId());

    List<OvertimeArchive> archives = overtimeArchiveRepository.findByUser_UserId(user.getUserId());
    assertThat(archives).hasSize(1);
    assertThat(archives.get(0).getType()).isEqualTo(OvertimeType.LOCKED);
    assertThat(archives.get(0).getWorkLog()).isNull();
  }

  @Test
  void registerPenaltyDebt_doesNothing_whenMissingSecondsIsZero() {
    WorkLog workLog = persistWorkLog(LocalDateTime.now().minusHours(1));

    overtimeService.registerPenaltyDebt(workLog, 0);

    assertThat(overtimeArchiveRepository.findByUser_UserId(user.getUserId())).isEmpty();
  }

  @Test
  void registerPenaltyDebt_createsDebtArchive_whenMissingSecondsPositive() {
    WorkLog workLog = persistWorkLog(LocalDateTime.now().minusHours(1));

    overtimeService.registerPenaltyDebt(workLog, 900);

    Integer balance = overtimeService.getOvertimeBalance(user.getUserId());
    assertThat(balance).isEqualTo(-900);
  }

  private User buildUser(String name, String surname, String email) {
    User newUser = new User();
    newUser.setName(name);
    newUser.setSurname(surname);
    newUser.setEmail(email);
    newUser.setPasswordHash("hash");
    return newUser;
  }

  private Application persistApplication(ApplicationType type, Integer durationSeconds) {
    Application application = new Application();
    application.setEmployee(user);
    application.setType(type);
    application.setStartDate(LocalDate.now());
    application.setEndDate(LocalDate.now());
    application.setDurationSeconds(durationSeconds);
    return applicationRepository.save(application);
  }

  private WorkLog persistWorkLog(LocalDateTime startTime) {
    WorkLog workLog = new WorkLog();
    workLog.setUser(user);
    workLog.setStartTime(startTime);
    return workLogRepository.save(workLog);
  }

  private OvertimeArchive persistArchive(OvertimeType type, int durationSeconds, WorkLog workLog,
                                         LocalDateTime validUntilOverride) {
    OvertimeArchive archive = new OvertimeArchive();
    archive.setUser(user);
    archive.setType(type);
    archive.setDurationSeconds(durationSeconds);
    archive.setWorkLog(workLog);
    archive.setValidUntil(
        validUntilOverride != null ? validUntilOverride : LocalDateTime.now().plusMonths(3));
    return overtimeArchiveRepository.save(archive);
  }
}
