package pl.chrono.dbmanager.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import pl.chrono.dbmanager.model.Role;
import pl.chrono.dbmanager.model.User;
import pl.chrono.dbmanager.repository.OperationLogRepository;
import pl.chrono.dbmanager.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class DataRetentionSchedulerTest {

  @Mock
  private OperationLogRepository operationLogRepository;

  @Mock
  private UserRepository userRepository;

  @InjectMocks
  private DataRetentionScheduler scheduler;

  @BeforeEach
  void setUp() {
    ReflectionTestUtils.setField(scheduler, "userRetentionYears", 10);
  }

  @Test
  void cleanupOperationLogsTest() {
    when(operationLogRepository.deleteOlderThan(any(LocalDateTime.class))).thenReturn(50);

    scheduler.cleanupOperationLogs();

    ArgumentCaptor<LocalDateTime> dateCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
    verify(operationLogRepository).deleteOlderThan(dateCaptor.capture());

    LocalDateTime capturedDate = dateCaptor.getValue();
    assertThat(capturedDate).isBetween(
        LocalDateTime.now().minusMonths(60).minusMinutes(1),
        LocalDateTime.now().minusMonths(60).plusMinutes(1)
    );
  }

  @Test
  void foundUserAnonymizeTest() {
    User oldUser = new User();
    oldUser.setUserId(1);
    oldUser.setName("Jan");
    oldUser.setSurname("Kowalski");
    oldUser.setEmail("jan.kowalski@example.com");
    oldUser.setPasswordHash("tajnehaslo123");
    oldUser.setRefreshToken("some-token");

    Role role = new Role();
    role.setName("ROLE_USER");
    oldUser.setRoles(new HashSet<>(List.of(role)));

    when(userRepository.findByIsActiveFalseAndEmploymentEndDateBefore(
        any(LocalDate.class))).thenReturn(List.of(oldUser));

    scheduler.anonymizeOldUsers();

    ArgumentCaptor<LocalDate> dateCaptor = ArgumentCaptor.forClass(LocalDate.class);
    verify(userRepository).findByIsActiveFalseAndEmploymentEndDateBefore(dateCaptor.capture());

    assertThat(dateCaptor.getValue()).isEqualTo(LocalDate.now().minusYears(10));

    ArgumentCaptor<List<User>> saveCaptor = ArgumentCaptor.forClass(List.class);
    verify(userRepository).saveAll(saveCaptor.capture());

    User savedUser = saveCaptor.getValue().getFirst();

    assertThat(savedUser.getName()).isEqualTo("Zanonimizowano");
    assertThat(savedUser.getSurname()).isEqualTo("Zanonimizowano");
    assertThat(savedUser.getEmail()).startsWith("anon_").endsWith("@deleted.chrono.pl");
    assertThat(savedUser.getPasswordHash()).isEqualTo("DELETED");
    assertThat(savedUser.getRefreshToken()).isNull();
    assertThat(savedUser.getRoles()).isEmpty();
  }

  @Test
  void emptyListTest() {
    when(userRepository.findByIsActiveFalseAndEmploymentEndDateBefore(
        any(LocalDate.class))).thenReturn(Collections.emptyList());

    scheduler.anonymizeOldUsers();

    verify(userRepository, never()).saveAll(any());
  }
}
