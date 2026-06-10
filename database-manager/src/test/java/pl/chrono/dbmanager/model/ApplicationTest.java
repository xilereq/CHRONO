package pl.chrono.dbmanager.model;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;
import pl.chrono.dbmanager.model.enums.ApplicationStatus;

class ApplicationTest {

  @Test
  void constructorTest() {
    Application application = new Application();

    assertThat(application.getStatus()).isEqualTo(ApplicationStatus.PENDING);
  }
}
