package pl.chrono.dbmanager.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import pl.chrono.dbmanager.model.Notification;

/**
 * Integration test for {@link NotificationRepository}.
 * Uses a real H2 in-memory database (via @DataJpaTest) to verify query behaviour.
 */
@DataJpaTest
class NotificationRepositoryIT {

  @Autowired
  private TestEntityManager em;

  @Autowired
  private NotificationRepository repository;

  @Test
  void findByUserIdAndIsReadFalseOrderByCreatedAtDesc_returnsUnreadNewestFirst() {
    Notification older = persistNotification(1, false, LocalDateTime.of(2026, 6, 1, 9, 0));
    Notification newer = persistNotification(1, false, LocalDateTime.of(2026, 6, 16, 9, 0));
    em.flush();

    List<Notification> result = repository
        .findByUserIdAndIsReadFalseOrderByCreatedAtDesc(1);

    assertThat(result).extracting(Notification::getId)
        .containsExactly(newer.getId(), older.getId());
  }

  @Test
  void findByUserIdAndIsReadFalseOrderByCreatedAtDesc_excludesReadNotifications() {
    persistNotification(1, true, LocalDateTime.now());
    em.flush();

    List<Notification> result = repository
        .findByUserIdAndIsReadFalseOrderByCreatedAtDesc(1);

    assertThat(result).isEmpty();
  }

  @Test
  void findByUserIdAndIsReadFalseOrderByCreatedAtDesc_excludesOtherUsers() {
    persistNotification(2, false, LocalDateTime.now());
    em.flush();

    List<Notification> result = repository
        .findByUserIdAndIsReadFalseOrderByCreatedAtDesc(1);

    assertThat(result).isEmpty();
  }

  @Test
  void findByUserIdAndIsReadFalseOrderByCreatedAtDesc_emptyWhenNoNotifications() {
    List<Notification> result = repository
        .findByUserIdAndIsReadFalseOrderByCreatedAtDesc(1);

    assertThat(result).isEmpty();
  }

  private Notification persistNotification(Integer userId, boolean isRead,
      LocalDateTime createdAt) {
    Notification notification = new Notification();
    notification.setUserId(userId);
    notification.setMessage("Test notification");
    notification.setRead(isRead);
    notification.setCreatedAt(createdAt);
    return em.persist(notification);
  }
}
