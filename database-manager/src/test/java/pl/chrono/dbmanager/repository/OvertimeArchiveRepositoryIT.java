package pl.chrono.dbmanager.repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import pl.chrono.dbmanager.model.Application;
import pl.chrono.dbmanager.model.OvertimeArchive;
import pl.chrono.dbmanager.model.User;
import pl.chrono.dbmanager.model.WorkLog;
import pl.chrono.dbmanager.model.enums.ApplicationStatus;
import pl.chrono.dbmanager.model.enums.ApplicationType;
import pl.chrono.dbmanager.model.enums.OvertimeType;

@DataJpaTest
class OvertimeArchiveRepositoryIT {

  @Autowired
  private TestEntityManager em;

  @Autowired
  private OvertimeArchiveRepository repository;

  private User user1;
  private User user2;

  @BeforeEach
  void setUp() {
    user1 = em.persist(buildUser("Jan", "Kowalski", "jan.kowalski@test.com"));
    user2 = em.persist(buildUser("Anna", "Nowak", "anna.nowak@test.com"));
    em.flush();
  }

  @Test
  void noRecordsOvertimeBalanceTest() {
    Integer balance = repository.calculateOvertimeBalanceSeconds(user1.getUserId());

    assertThat(balance).isZero();
  }

  @Test
  void calculateSumOfOvertimeRecordsTest() {
    persistOvertime(user1, OvertimeType.EARNED, 3600);
    persistOvertime(user1, OvertimeType.MANUAL_ADDITION, 1800);
    persistOvertime(user1, OvertimeType.REFUNDED, 900);
    em.flush();

    Integer balance = repository.calculateOvertimeBalanceSeconds(user1.getUserId());

    assertThat(balance).isEqualTo(6300);
  }

  @Test
  void calculateDebtOfOvertimeRecordsTest() {
    persistOvertime(user1, OvertimeType.USED, 1800);
    persistOvertime(user1, OvertimeType.EXPIRED, 600);
    em.flush();

    Integer balance = repository.calculateOvertimeBalanceSeconds(user1.getUserId());

    assertThat(balance).isEqualTo(-2400);
  }

  @Test
  void calculateMixedTypesOvertime() {
    persistOvertime(user1, OvertimeType.EARNED, 7200);          // +
    persistOvertime(user1, OvertimeType.MANUAL_ADDITION, 1800); // +
    persistOvertime(user1, OvertimeType.USED, 3600);            // -
    persistOvertime(user1, OvertimeType.DEBT, 600);             // -
    em.flush();

    Integer balance = repository.calculateOvertimeBalanceSeconds(user1.getUserId());

    assertThat(balance).isEqualTo(4800);
  }

  @Test
  void calculateIncludesOnlyYourRecordsTest() {
    persistOvertime(user1, OvertimeType.EARNED, 3600);
    persistOvertime(user2, OvertimeType.EARNED, 9999);
    em.flush();

    Integer balance = repository.calculateOvertimeBalanceSeconds(user1.getUserId());

    assertThat(balance).isEqualTo(3600);
  }

  @Test
  void sumEarnedOvertimeBetweenDateRangeTest() {
    LocalDateTime startOfYear = LocalDateTime.of(2026, 1, 1, 0, 0);
    LocalDateTime endOfYear = LocalDateTime.of(2026, 12, 31, 23, 59, 59);

    WorkLog wl1 = persistWorkLog(user1, LocalDateTime.of(2026, 5, 10, 15, 0));
    WorkLog wl2 = persistWorkLog(user1, LocalDateTime.of(2026, 8, 20, 10, 0));
    WorkLog wl3 = persistWorkLog(user1, LocalDateTime.of(2025, 12, 31, 23, 0));

    persistOvertimeWithWorkLog(user1, OvertimeType.EARNED, 3600, wl1); // +
    persistOvertimeWithWorkLog(user1, OvertimeType.EARNED, 7200, wl2); // +
    persistOvertimeWithWorkLog(user1, OvertimeType.EARNED, 1800, wl3);
    em.flush();

    Integer sum = repository.sumEarnedOvertimeBetween(user1.getUserId(), startOfYear, endOfYear);
    assertThat(sum).isEqualTo(10800);
  }

  @Test
  void sumEarnedOvertimeBetweenOtherOvertimeTypesTest() {
    LocalDateTime startOfYear = LocalDateTime.of(2026, 1, 1, 0, 0);
    LocalDateTime endOfYear = LocalDateTime.of(2026, 12, 31, 23, 59, 59);

    WorkLog wl1 = persistWorkLog(user1, LocalDateTime.of(2026, 5, 10, 15, 0));
    WorkLog wl2 = persistWorkLog(user1, LocalDateTime.of(2026, 8, 20, 10, 0));
    WorkLog wl3 = persistWorkLog(user1, LocalDateTime.of(2026, 12, 31, 23, 0));

    persistOvertimeWithWorkLog(user1, OvertimeType.EARNED, 3600, wl1); // +
    persistOvertimeWithWorkLog(user1, OvertimeType.MANUAL_ADDITION, 7200, wl2); // +
    persistOvertimeWithWorkLog(user1, OvertimeType.DEBT, 1800, wl3);
    em.flush();

    Integer sum = repository.sumEarnedOvertimeBetween(user1.getUserId(), startOfYear, endOfYear);
    assertThat(sum).isEqualTo(10800);
  }

  @Test
  void sumEarnedOvertimeBetweenDateRangeNoRecordsTest() {
    LocalDateTime startOfYear = LocalDateTime.of(2026, 1, 1, 0, 0);
    LocalDateTime endOfYear = LocalDateTime.of(2026, 12, 31, 23, 59, 59);

    Integer sum = repository.sumEarnedOvertimeBetween(user1.getUserId(), startOfYear, endOfYear);
    assertThat(sum).isZero();
  }

  @Test
  void sumEarnedOvertimeBetweenDateRangeNoEarnedRecordsTest() {
    LocalDateTime startOfYear = LocalDateTime.of(2026, 1, 1, 0, 0);
    LocalDateTime endOfYear = LocalDateTime.of(2026, 12, 31, 23, 59, 59);

    WorkLog wl1 = persistWorkLog(user1, LocalDateTime.of(2026, 5, 10, 15, 0));
    WorkLog wl2 = persistWorkLog(user1, LocalDateTime.of(2026, 8, 20, 10, 0));

    persistOvertimeWithWorkLog(user1, OvertimeType.EXPIRED, 7200, wl1);
    persistOvertimeWithWorkLog(user1, OvertimeType.DEBT, 1800, wl2);
    em.flush();

    Integer sum = repository.sumEarnedOvertimeBetween(user1.getUserId(), startOfYear, endOfYear);
    assertThat(sum).isZero();
  }

  @Test
  void sumEarnedOvertimeBetweenDateRangeOtherUserRecordsTest() {
    LocalDateTime startOfYear = LocalDateTime.of(2026, 1, 1, 0, 0);
    LocalDateTime endOfYear = LocalDateTime.of(2026, 12, 31, 23, 59, 59);

    WorkLog wl1 = persistWorkLog(user1, LocalDateTime.of(2026, 5, 10, 15, 0));
    WorkLog wl2 = persistWorkLog(user2, LocalDateTime.of(2026, 8, 20, 10, 0));

    persistOvertimeWithWorkLog(user1, OvertimeType.EARNED, 3600, wl1); // +
    persistOvertimeWithWorkLog(user2, OvertimeType.EARNED, 7200, wl2);
    em.flush();

    Integer sum = repository.sumEarnedOvertimeBetween(user1.getUserId(), startOfYear, endOfYear);
    assertThat(sum).isEqualTo(3600);
  }

  @Test
  void emptyListTest() {
    List<OvertimeArchive> result = repository.findByUser_UserId(user1.getUserId());

    assertThat(result).isEmpty();
  }

  @Test
  void onlyFindYourRecordsTest() {
    OvertimeArchive a1 = persistOvertime(user1, OvertimeType.EARNED, 3600);
    OvertimeArchive a2 = persistOvertime(user1, OvertimeType.USED, 1800);
    persistOvertime(user2, OvertimeType.EARNED, 7200);
    em.flush();

    List<OvertimeArchive> result = repository.findByUser_UserId(user1.getUserId());

    assertThat(result).hasSize(2).extracting(OvertimeArchive::getId)
        .containsExactlyInAnyOrder(a1.getId(), a2.getId());
  }

  @Test
  void findRecordByApplicationTest() {
    Application app = em.persist(buildApplication(user1));
    OvertimeArchive archive = persistOvertimeWithApplication(user1, OvertimeType.LOCKED, 1800, app);
    em.flush();

    Optional<OvertimeArchive> result = repository.findByApplication_IdAndType(
        app.getId(), OvertimeType.LOCKED);

    assertThat(result).isPresent().get().extracting(OvertimeArchive::getId)
        .isEqualTo(archive.getId());
  }

  @Test
  void emptyListByWrongTypeTest() {
    Application app = em.persist(buildApplication(user1));
    persistOvertimeWithApplication(user1, OvertimeType.LOCKED, 1800, app);
    em.flush();

    Optional<OvertimeArchive> result = repository.findByApplication_IdAndType(
        app.getId(), OvertimeType.USED);

    assertThat(result).isEmpty();
  }

  @Test
  void emptyListByWrongApplicationIdTest() {
    Application app = em.persist(buildApplication(user1));
    persistOvertimeWithApplication(user1, OvertimeType.LOCKED, 1800, app);
    em.flush();

    Optional<OvertimeArchive> result = repository.findByApplication_IdAndType(
        999L, OvertimeType.LOCKED);

    assertThat(result).isEmpty();
  }

  @Test
  void calculateExpiringBalanceSecondsTest() {
    LocalDateTime threshold = LocalDateTime.of(2026, 6, 30, 23, 59, 59);

    persistOvertimeWithValidUntil(
        user1, OvertimeType.EARNED, 3600, LocalDateTime.of(2026, 5, 1, 10, 0));
    persistOvertimeWithValidUntil(user1, OvertimeType.MANUAL_ADDITION, 1800, threshold);

    persistOvertimeWithValidUntil(
        user1, OvertimeType.USED, 900, LocalDateTime.of(2026, 4, 1, 10, 0));

    persistOvertimeWithValidUntil(
        user1, OvertimeType.EARNED, 7200, LocalDateTime.of(2026, 7, 1, 10, 0));
    em.flush();

    Integer balance = repository.calculateExpiringBalanceSeconds(user1.getUserId(), threshold);

    assertThat(balance).isEqualTo(4500);
  }

  @Test
  void calculateExpiringBalanceSecondsNoRecordsTest() {
    Integer balance = repository.calculateExpiringBalanceSeconds(
        user1.getUserId(), LocalDateTime.now());
    assertThat(balance).isZero();
  }

  @Test
  void calculateExpiringBalanceSecondsOtherUserTest() {
    LocalDateTime threshold = LocalDateTime.now().plusDays(10);
    persistOvertimeWithValidUntil(user2, OvertimeType.EARNED, 3600, LocalDateTime.now());
    em.flush();

    Integer balance = repository.calculateExpiringBalanceSeconds(user1.getUserId(), threshold);

    assertThat(balance).isZero();
  }

  @Test
  void findByWorkLogIdTest() {
    WorkLog wl1 = persistWorkLog(user1, LocalDateTime.now());
    WorkLog wl2 = persistWorkLog(user1, LocalDateTime.now());

    OvertimeArchive a1 = persistOvertimeWithWorkLog(user1, OvertimeType.EARNED, 3600, wl1);
    OvertimeArchive a2 = persistOvertimeWithWorkLog(user1, OvertimeType.USED, 1800, wl1);

    persistOvertimeWithWorkLog(user1, OvertimeType.EARNED, 7200, wl2);
    em.flush();

    List<OvertimeArchive> result = repository.findByWorkLog_WorkLogId(wl1.getWorkLogId());

    assertThat(result).hasSize(2).extracting(OvertimeArchive::getId)
        .containsExactlyInAnyOrder(a1.getId(), a2.getId());
  }

  @Test
  void findByWorkLogIdEmptyTest() {
    List<OvertimeArchive> result = repository.findByWorkLog_WorkLogId(999);

    assertThat(result).isEmpty();
  }

  private User buildUser(String name, String surname, String email) {
    User user = new User();
    user.setName(name);
    user.setSurname(surname);
    user.setEmail(email);
    user.setPasswordHash("hash");
    return user;
  }

  private Application buildApplication(User employee) {
    Application app = new Application();
    app.setEmployee(employee);
    app.setType(ApplicationType.OVERTIME_EARN);
    app.setStatus(ApplicationStatus.PENDING);
    app.setStartDate(LocalDate.now());
    app.setEndDate(LocalDate.now().plusDays(1));
    app.setCreationDateTime(LocalDateTime.now());
    return app;
  }

  private OvertimeArchive persistOvertime(User user, OvertimeType type, int durationSeconds) {
    OvertimeArchive archive = new OvertimeArchive();
    archive.setUser(user);
    archive.setType(type);
    archive.setDurationSeconds(durationSeconds);
    archive.setValidUntil(LocalDateTime.now().plusYears(1));
    return em.persist(archive);
  }

  private OvertimeArchive persistOvertimeWithApplication(User user, OvertimeType type,
                                                         int durationSeconds,
                                                         Application application) {
    OvertimeArchive archive = new OvertimeArchive();
    archive.setUser(user);
    archive.setType(type);
    archive.setDurationSeconds(durationSeconds);
    archive.setValidUntil(LocalDateTime.now().plusYears(1));
    archive.setApplication(application);
    return em.persist(archive);
  }

  private WorkLog persistWorkLog(User user, LocalDateTime startTime) {
    WorkLog workLog = new WorkLog();
    workLog.setUser(user);
    workLog.setStartTime(startTime);
    return em.persist(workLog);
  }

  private OvertimeArchive persistOvertimeWithWorkLog(User user, OvertimeType type,
                                                     int durationSeconds, WorkLog workLog) {
    OvertimeArchive archive = persistOvertime(user, type, durationSeconds);
    archive.setWorkLog(workLog);
    return em.persist(archive);
  }

  private OvertimeArchive persistOvertimeWithValidUntil(User user, OvertimeType type,
                                                        int durationSeconds,
                                                        LocalDateTime validUntil) {
    OvertimeArchive archive = new OvertimeArchive();
    archive.setUser(user);
    archive.setType(type);
    archive.setDurationSeconds(durationSeconds);
    archive.setValidUntil(validUntil);
    return em.persist(archive);
  }
}
