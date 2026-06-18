package pl.chrono.dbmanager.repository;

import java.time.LocalDateTime;
import java.util.Optional;
import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import pl.chrono.dbmanager.model.LeaveLog;
import pl.chrono.dbmanager.model.User;
import pl.chrono.dbmanager.model.WorkLog;

@DataJpaTest
class LeaveLogRepositoryIT {

  @Autowired
  private TestEntityManager em;

  @Autowired
  private LeaveLogRepository repository;

  private User user1;
  private User user2;

  @BeforeEach
  void setUp() {
    user1 = em.persist(buildUser("Jan", "Kowalski", "jan.kowalski@test.com"));
    user2 = em.persist(buildUser("Anna", "Nowak", "anna.nowak@test.com"));
    em.flush();
  }

  @Test
  void findByUser_UserIdAndEndTimeIsNull_returnsActiveLeave() {
    LeaveLog active = persistLeaveLog(user1, LocalDateTime.of(2026, 6, 16, 12, 0), null);
    em.flush();

    Optional<LeaveLog> result = repository.findByUser_UserIdAndEndTimeIsNull(user1.getUserId());

    assertThat(result).isPresent();
    assertThat(result.get().getLeaveLogId()).isEqualTo(active.getLeaveLogId());
  }

  @Test
  void findByUser_UserIdAndEndTimeIsNull_ignoresClosedLeave() {
    persistLeaveLog(
        user1, LocalDateTime.of(2026, 6, 16, 12, 0),
        LocalDateTime.of(2026, 6, 16, 12, 30)
    );
    em.flush();

    Optional<LeaveLog> result = repository.findByUser_UserIdAndEndTimeIsNull(user1.getUserId());

    assertThat(result).isEmpty();
  }

  @Test
  void findByUser_UserIdAndEndTimeIsNull_excludesOtherUsers() {
    persistLeaveLog(user1, LocalDateTime.of(2026, 6, 16, 12, 0), null);
    em.flush();

    Optional<LeaveLog> result = repository.findByUser_UserIdAndEndTimeIsNull(user2.getUserId());

    assertThat(result).isEmpty();
  }

  @Test
  void existsByUser_UserIdAndEndTimeIsNull_trueWhenActiveLeaveExists() {
    persistLeaveLog(user1, LocalDateTime.of(2026, 6, 16, 12, 0), null);
    em.flush();

    boolean exists = repository.existsByUser_UserIdAndEndTimeIsNull(user1.getUserId());

    assertThat(exists).isTrue();
  }

  @Test
  void existsByUser_UserIdAndEndTimeIsNull_falseWhenNoActiveLeave() {
    persistLeaveLog(
        user1, LocalDateTime.of(2026, 6, 16, 12, 0),
        LocalDateTime.of(2026, 6, 16, 12, 30)
    );
    em.flush();

    boolean exists = repository.existsByUser_UserIdAndEndTimeIsNull(user1.getUserId());

    assertThat(exists).isFalse();
  }

  @Test
  void existsByUser_UserIdAndEndTimeIsNull_falseWhenNoRecordsAtAll() {
    boolean exists = repository.existsByUser_UserIdAndEndTimeIsNull(user1.getUserId());

    assertThat(exists).isFalse();
  }

  @Test
  void leaveLogCanBeLinkedToWorkLog() {
    WorkLog workLog = em.persist(buildWorkLog(user1, LocalDateTime.of(2026, 6, 16, 8, 0)));
    LeaveLog leave = persistLeaveLogWithWorkLog(
        user1, LocalDateTime.of(2026, 6, 16, 12, 0), null,
        workLog
    );
    em.flush();

    Optional<LeaveLog> result = repository.findByUser_UserIdAndEndTimeIsNull(user1.getUserId());

    assertThat(result).isPresent();
    assertThat(result.get().getWorkLog().getWorkLogId()).isEqualTo(workLog.getWorkLogId());
  }

  private User buildUser(String name, String surname, String email) {
    User user = new User();
    user.setName(name);
    user.setSurname(surname);
    user.setEmail(email);
    user.setPasswordHash("hash");
    return user;
  }

  private WorkLog buildWorkLog(User user, LocalDateTime startTime) {
    WorkLog workLog = new WorkLog();
    workLog.setUser(user);
    workLog.setStartTime(startTime);
    return workLog;
  }

  private LeaveLog persistLeaveLog(User user, LocalDateTime startTime, LocalDateTime endTime) {
    LeaveLog leaveLog = new LeaveLog();
    leaveLog.setUser(user);
    leaveLog.setStartTime(startTime);
    leaveLog.setEndTime(endTime);
    return em.persist(leaveLog);
  }

  private LeaveLog persistLeaveLogWithWorkLog(User user, LocalDateTime startTime,
                                              LocalDateTime endTime, WorkLog workLog) {
    LeaveLog leaveLog = new LeaveLog();
    leaveLog.setUser(user);
    leaveLog.setStartTime(startTime);
    leaveLog.setEndTime(endTime);
    leaveLog.setWorkLog(workLog);
    return em.persist(leaveLog);
  }
}
