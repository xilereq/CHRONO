package pl.chrono.dbmanager.model;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;

class OvertimeArchiveTest {

  @Test
  void constructorTest() {
    OvertimeArchive archive = new OvertimeArchive();

    assertThat(archive.getCreatedAt()).isNotNull();
  }
}
