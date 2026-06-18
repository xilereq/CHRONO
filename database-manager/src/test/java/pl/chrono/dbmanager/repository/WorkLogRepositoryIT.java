package pl.chrono.dbmanager.repository;

import java.time.LocalDateTime;
import java.util.Optional;
import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import pl.chrono.dbmanager.model.User;
import pl.chrono.dbmanager.model.WorkLog;
import pl.chrono.dbmanager.model.enums.WorkLocationType;

@DataJpaTest
class WorkLogRepositoryIT {

  @Autowired
  private TestEntityManager em;

  @Autowired
  private WorkLogRepository repository;

  private User employee;

  @BeforeEach
  void setUp() {
    User user = new User();
    user.setName("Test");
    user.setSurname("User");
    user.setEmail("test.user@test.com");
    user.setPasswordHash("hash");
    employee = em.persistAndFlush(user);
  }

  @Test
  void findByUserUserIdAndStartTimeBetween_returnsLogWhenValidAndNotPendingCorrection() {
    WorkLog workLog = buildWorkLog(employee, LocalDateTime.of(2026, 6, 18, 8, 0));
    workLog.setPendingCorrection(false);
    em.persistAndFlush(workLog);

    Optional<WorkLog> result = repository.findByUserUserIdAndStartTimeBetween(
        employee.getUserId(),
        LocalDateTime.of(2026, 6, 18, 0, 0),
        LocalDateTime.of(2026, 6, 18, 23, 59)
    );

    assertThat(result).isPresent();
    assertThat(result.get().getWorkLogId()).isEqualTo(workLog.getWorkLogId());
  }

  @Test
  void findByUserUserIdAndStartTimeBetween_returnsEmptyWhenPendingCorrectionIsTrue() {
    WorkLog workLog = buildWorkLog(employee, LocalDateTime.of(2026, 6, 18, 8, 0));
    workLog.setPendingCorrection(true);
    em.persistAndFlush(workLog);

    Optional<WorkLog> result = repository.findByUserUserIdAndStartTimeBetween(
        employee.getUserId(),
        LocalDateTime.of(2026, 6, 18, 0, 0),
        LocalDateTime.of(2026, 6, 18, 23, 59)
    );

    assertThat(result).isEmpty();
  }

  @Test
  void findByUserUserIdAndStartTimeBetween_returnsEmptyWhenOutsideTimeRange() {
    WorkLog workLog = buildWorkLog(employee, LocalDateTime.of(2026, 6, 17, 8, 0));
    workLog.setPendingCorrection(false);
    em.persistAndFlush(workLog);

    Optional<WorkLog> result = repository.findByUserUserIdAndStartTimeBetween(
        employee.getUserId(),
        LocalDateTime.of(2026, 6, 18, 0, 0),
        LocalDateTime.of(2026, 6, 18, 23, 59)
    );

    assertThat(result).isEmpty();
  }

  @Test
  void findByUserUserIdAndStartTimeBetween_returnsEmptyForDifferentUser() {
    User otherEmployee = new User();
    otherEmployee.setName("Other");
    otherEmployee.setSurname("User");
    otherEmployee.setEmail("other@test.com");
    otherEmployee.setPasswordHash("hash");
    otherEmployee = em.persistAndFlush(otherEmployee);

    WorkLog workLog = buildWorkLog(otherEmployee, LocalDateTime.of(2026, 6, 18, 8, 0));
    workLog.setPendingCorrection(false);
    em.persistAndFlush(workLog);

    Optional<WorkLog> result = repository.findByUserUserIdAndStartTimeBetween(
        employee.getUserId(),
        LocalDateTime.of(2026, 6, 18, 0, 0),
        LocalDateTime.of(2026, 6, 18, 23, 59)
    );

    assertThat(result).isEmpty();
  }

  private WorkLog buildWorkLog(User user, LocalDateTime startTime) {
    WorkLog workLog = new WorkLog();
    workLog.setUser(user);
    workLog.setStartTime(startTime);
    workLog.setLocationType(WorkLocationType.OFFICE);
    return workLog;
  }
}
