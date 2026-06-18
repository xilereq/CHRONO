package pl.chrono.dbmanager.repository;

import java.time.LocalDateTime;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import pl.chrono.dbmanager.model.User;
import pl.chrono.dbmanager.model.WorkLogRequest;
import pl.chrono.dbmanager.model.enums.WorkLogRequestStatus;

@DataJpaTest
class WorkLogRequestRepositoryIT {

  @Autowired
  private TestEntityManager em;

  @Autowired
  private WorkLogRequestRepository repository;

  private User employee1;
  private User employee2;
  private User manager1;
  private User manager2;

  @BeforeEach
  void setUp() {
    manager1 = em.persist(buildUser("Marek", "Kierownik", "marek.kierownik@test.com"));
    manager2 = em.persist(buildUser("Ola", "Szefowa", "ola.szefowa@test.com"));
    employee1 = em.persist(buildUser("Jan", "Kowalski", "jan.kowalski@test.com"));
    employee1.setManager(manager1);
    employee2 = em.persist(buildUser("Anna", "Nowak", "anna.nowak@test.com"));
    employee2.setManager(manager2);
    em.flush();
  }

  @Test
  void findAllByEmployeeUserIdOrderByCreatedAtDesc_returnsRequestsNewestFirst() {
    WorkLogRequest older = persistRequest(employee1, manager1, WorkLogRequestStatus.PENDING,
        LocalDateTime.of(2026, 6, 1, 10, 0));
    WorkLogRequest newer = persistRequest(employee1, manager1, WorkLogRequestStatus.PENDING,
        LocalDateTime.of(2026, 6, 10, 10, 0));
    em.flush();

    List<WorkLogRequest> result = repository.findAllByEmployeeUserIdOrderByCreatedAtDesc(
        employee1.getUserId());

    assertThat(result).extracting(WorkLogRequest::getId)
        .containsExactly(newer.getId(), older.getId());
  }

  @Test
  void findAllByEmployeeUserIdOrderByCreatedAtDesc_excludesOtherEmployees() {
    persistRequest(employee2, manager2, WorkLogRequestStatus.PENDING, LocalDateTime.now());
    em.flush();

    List<WorkLogRequest> result = repository.findAllByEmployeeUserIdOrderByCreatedAtDesc(
        employee1.getUserId());

    assertThat(result).isEmpty();
  }

  @Test
  void findAllByEmployeeManagerUserIdAndStatus_returnsOnlyMatchingStatus() {
    WorkLogRequest pending = persistRequest(employee1, manager1, WorkLogRequestStatus.PENDING,
        LocalDateTime.now());
    persistRequest(employee1, manager1, WorkLogRequestStatus.APPROVED, LocalDateTime.now());
    em.flush();

    List<WorkLogRequest> result = repository.findAllByEmployeeManagerUserIdAndStatus(
        manager1.getUserId(), WorkLogRequestStatus.PENDING);

    assertThat(result).extracting(WorkLogRequest::getId)
        .containsExactly(pending.getId());
  }

  @Test
  void findAllByEmployeeManagerUserIdAndStatus_excludesOtherManagersSubordinates() {
    persistRequest(employee2, manager2, WorkLogRequestStatus.PENDING, LocalDateTime.now());
    em.flush();

    List<WorkLogRequest> result = repository.findAllByEmployeeManagerUserIdAndStatus(
        manager1.getUserId(), WorkLogRequestStatus.PENDING);

    assertThat(result).isEmpty();
  }

  @Test
  void findAllByEmployeeManagerUserIdAndStatus_emptyWhenNoneMatch() {
    persistRequest(employee1, manager1, WorkLogRequestStatus.REJECTED, LocalDateTime.now());
    em.flush();

    List<WorkLogRequest> result = repository.findAllByEmployeeManagerUserIdAndStatus(
        manager1.getUserId(), WorkLogRequestStatus.PENDING);

    assertThat(result).isEmpty();
  }

  @Test
  void findAllByStatus_returnsAllPendingAcrossManagers() {
    WorkLogRequest pending1 = persistRequest(employee1, manager1, WorkLogRequestStatus.PENDING,
        LocalDateTime.now());
    WorkLogRequest pending2 = persistRequest(employee2, manager2, WorkLogRequestStatus.PENDING,
        LocalDateTime.now());
    persistRequest(employee1, manager1, WorkLogRequestStatus.APPROVED, LocalDateTime.now());
    em.flush();

    List<WorkLogRequest> result = repository.findAllByStatus(WorkLogRequestStatus.PENDING);

    assertThat(result).extracting(WorkLogRequest::getId)
        .containsExactlyInAnyOrder(pending1.getId(), pending2.getId());
  }

  @Test
  void findAllByStatus_emptyWhenNoneMatch() {
    persistRequest(employee1, manager1, WorkLogRequestStatus.APPROVED, LocalDateTime.now());
    em.flush();

    List<WorkLogRequest> result = repository.findAllByStatus(WorkLogRequestStatus.REJECTED);

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

  private WorkLogRequest persistRequest(User employee, User manager,
                                        WorkLogRequestStatus status, LocalDateTime createdAt) {
    WorkLogRequest request = new WorkLogRequest();
    request.setEmployee(employee);
    request.setManager(manager);
    request.setStatus(status);
    request.setProposedStartTime(LocalDateTime.of(2026, 6, 16, 8, 0));
    request.setProposedEndTime(LocalDateTime.of(2026, 6, 16, 16, 0));
    request.setCreatedAt(createdAt);
    return em.persist(request);
  }
}
