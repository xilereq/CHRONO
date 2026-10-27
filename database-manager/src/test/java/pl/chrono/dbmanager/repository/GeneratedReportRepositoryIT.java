package pl.chrono.dbmanager.repository;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import pl.chrono.dbmanager.model.GeneratedReport;
import pl.chrono.dbmanager.model.GeneratedReportInfo;
import pl.chrono.dbmanager.model.User;
import pl.chrono.dbmanager.model.enums.ReportType;

@DataJpaTest
class GeneratedReportRepositoryIT {

  @Autowired
  private TestEntityManager em;

  @Autowired
  private GeneratedReportRepository repository;

  @Test
  void savesAndLoadsFileContent() {
    User user = persistUser("report-owner@chrono.pl");
    byte[] content = "Data;Start\n2026-10-01;08:00\n".getBytes(StandardCharsets.UTF_8);
    GeneratedReport report = persistReport(user, ReportType.TIMESHEET, content, LocalDateTime.now());
    em.flush();
    em.clear();

    GeneratedReport loaded = repository.findById(report.getId()).orElseThrow();

    assertThat(loaded.getContent()).isEqualTo(content);
    assertThat(loaded.getFileSize()).isEqualTo(content.length);
    assertThat(loaded.getReportType()).isEqualTo(ReportType.TIMESHEET);
    assertThat(loaded.getSubjectUserId()).isEqualTo(user.getUserId());
    assertThat(loaded.getRequestedByUserId()).isNull();
  }

  @Test
  void findBySubjectUserIdOrderByCreatedAtDesc_returnsUsersReportsNewestFirst() {
    User user = persistUser("history@chrono.pl");
    User other = persistUser("other@chrono.pl");
    byte[] content = {1, 2, 3};
    GeneratedReport older = persistReport(user, ReportType.TIMESHEET, content,
        LocalDateTime.of(2026, 9, 1, 6, 0));
    GeneratedReport newer = persistReport(user, ReportType.EMPLOYEE_OVERTIME, content,
        LocalDateTime.of(2026, 10, 1, 6, 0));
    persistReport(other, ReportType.TIMESHEET, content, LocalDateTime.of(2026, 10, 2, 6, 0));
    em.flush();

    List<GeneratedReportInfo> history = repository.findBySubjectUserIdOrderByCreatedAtDesc(user.getUserId());

    assertThat(history).extracting(GeneratedReportInfo::getId)
        .containsExactly(newer.getId(), older.getId());
    assertThat(history.getFirst().getReportType()).isEqualTo(ReportType.EMPLOYEE_OVERTIME);
    assertThat(history.getFirst().getFileName()).isEqualTo("report.csv");
  }

  private GeneratedReport persistReport(User subject, ReportType type, byte[] content,
                                        LocalDateTime createdAt) {
    GeneratedReport report = new GeneratedReport();
    report.setReportType(type);
    report.setFormat("CSV");
    report.setPeriodStart(LocalDate.of(2026, 9, 1));
    report.setPeriodEnd(LocalDate.of(2026, 9, 30));
    report.setSubjectUserId(subject.getUserId());
    report.setFileName("report.csv");
    report.setContentType("text/csv; charset=UTF-8");
    report.setFileSize(content.length);
    report.setContent(content);
    report.setCreatedAt(createdAt);
    return em.persist(report);
  }

  private User persistUser(String email) {
    User user = new User();
    user.setEmail(email);
    user.setPasswordHash("hash");
    user.setName("Test");
    user.setSurname("User");
    user.setActive(true);
    return em.persist(user);
  }
}
