package pl.chrono.timemanager.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import pl.chrono.dbmanager.model.User;
import pl.chrono.dbmanager.model.WorkLog;
import pl.chrono.dbmanager.repository.LeaveLogRepository;
import pl.chrono.dbmanager.repository.UserRepository;
import pl.chrono.dbmanager.repository.WorkLogRepository;
import pl.chrono.timemanager.TestConfig;
import pl.chrono.timemanager.dto.response.LeaveLogDto;
import pl.chrono.timemanager.exception.WorkException;

/**
 * Integration test for {@link LeaveLogService}.
 * Runs against a real Spring context wired to a real H2 database (no mocks for
 * I_TimeData/UserRepository) to verify the full
 * service -&gt; adapter -&gt; repository -&gt; database chain.
 */
@SpringBootTest(classes = TestConfig.class)
@Transactional
class LeaveLogServiceIT {

  @Autowired
  private LeaveLogService leaveLogService;

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private WorkLogRepository workLogRepository;

  @Autowired
  private LeaveLogRepository leaveLogRepository;

  private User user;

  @BeforeEach
  void setUp() {
    user = userRepository.save(buildUser("Jan", "Kowalski", "jan.kowalski@test.com"));
  }

  @Test
  void startLeave_createsActiveLeaveLog_whenUserIsWorking() {
    persistActiveWorkLog();

    LeaveLogDto result = leaveLogService.startLeave(user.getUserId());

    assertThat(result.getLeaveLogId()).isNotNull();
    assertThat(result.getEndTime()).isNull();
    assertThat(leaveLogService.isUserOnLeave(user.getUserId())).isTrue();
  }

  @Test
  void startLeave_throwsNoActiveWork_whenUserIsNotWorking() {
    assertThatThrownBy(() -> leaveLogService.startLeave(user.getUserId()))
        .isInstanceOf(WorkException.NoActiveWork.class);
  }

  @Test
  void startLeave_throwsLeaveStarted_whenAlreadyOnLeave() {
    persistActiveWorkLog();
    leaveLogService.startLeave(user.getUserId());

    assertThatThrownBy(() -> leaveLogService.startLeave(user.getUserId()))
        .isInstanceOf(WorkException.LeaveStarted.class);
  }

  @Test
  void startLeave_linksLeaveLogToTheActiveWorkLog() {
    WorkLog activeWorkLog = persistActiveWorkLog();

    leaveLogService.startLeave(user.getUserId());

    var persisted = leaveLogRepository.findByUser_UserIdAndEndTimeIsNull(user.getUserId())
        .orElseThrow();
    assertThat(persisted.getWorkLog().getWorkLogId()).isEqualTo(activeWorkLog.getWorkLogId());
  }

  @Test
  void endLeave_closesActiveLeaveLog() {
    persistActiveWorkLog();
    leaveLogService.startLeave(user.getUserId());

    LeaveLogDto result = leaveLogService.endLeave(user.getUserId());

    assertThat(result.getEndTime()).isNotNull();
    assertThat(leaveLogService.isUserOnLeave(user.getUserId())).isFalse();
  }

  @Test
  void endLeave_throwsNoActiveLeave_whenNothingToClose() {
    assertThatThrownBy(() -> leaveLogService.endLeave(user.getUserId()))
        .isInstanceOf(WorkException.NoActiveLeave.class);
  }

  @Test
  void isUserOnLeave_falseByDefault() {
    assertThat(leaveLogService.isUserOnLeave(user.getUserId())).isFalse();
  }

  @Test
  void isUserOnLeave_falseAfterLeaveIsClosed() {
    persistActiveWorkLog();
    leaveLogService.startLeave(user.getUserId());
    leaveLogService.endLeave(user.getUserId());

    assertThat(leaveLogService.isUserOnLeave(user.getUserId())).isFalse();
  }

  private User buildUser(String name, String surname, String email) {
    User newUser = new User();
    newUser.setName(name);
    newUser.setSurname(surname);
    newUser.setEmail(email);
    newUser.setPasswordHash("hash");
    return newUser;
  }

  private WorkLog persistActiveWorkLog() {
    WorkLog workLog = new WorkLog();
    workLog.setUser(user);
    workLog.setStartTime(LocalDateTime.now().minusHours(1));
    return workLogRepository.save(workLog);
  }
}
