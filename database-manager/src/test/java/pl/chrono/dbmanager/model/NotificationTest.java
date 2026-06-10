package pl.chrono.dbmanager.model;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;

class NotificationTest {

  @Test
  void constructorTest() {
    Notification notification = new Notification();

    assertThat(notification.isRead()).isFalse();
    assertThat(notification.getCreatedAt()).isNotNull();
  }
}
