package pl.chrono.reportmanager.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.transaction.annotation.Transactional;
import pl.chrono.dbmanager.model.Role;
import pl.chrono.dbmanager.model.ScheduledReport;
import pl.chrono.dbmanager.model.User;
import pl.chrono.dbmanager.model.enums.ReportPeriod;
import pl.chrono.dbmanager.model.enums.ScheduleFrequency;
import pl.chrono.dbmanager.model.enums.ScheduleRunStatus;
import pl.chrono.dbmanager.model.enums.ScheduledReportType;
import pl.chrono.dbmanager.repository.RoleRepository;
import pl.chrono.dbmanager.repository.ScheduledReportRepository;
import pl.chrono.dbmanager.repository.UserRepository;
import pl.chrono.reportmanager.TestConfig;
import pl.chrono.reportmanager.api.I_ScheduledReportService;
import pl.chrono.reportmanager.event.ReportAttachmentDto;
import pl.chrono.reportmanager.event.ScheduledReportEmailEvent;

@SpringBootTest(classes = TestConfig.class)
@Transactional
@RecordApplicationEvents
class ScheduledReportGenerationIT {

  @Autowired private I_ScheduledReportService scheduledReportService;
  @Autowired private ScheduledReportRepository scheduledReportRepository;
  @Autowired private UserRepository userRepository;
  @Autowired private RoleRepository roleRepository;
  @Autowired private ApplicationEvents events;

  private User manager;

  @BeforeEach
  void givenManagerWithOneEmployee() {
    manager = persistUser("Marek", "Manager", "marek.manager@chrono.pl", "ROLE_MANAGER", null);
    persistUser("Jan", "Kowalski", "jan.kowalski@chrono.pl", "ROLE_EMPLOYEE", manager);
  }

  @Test
  void dueScheduleSendsGeneratedPdfFile() {
    ScheduledReport schedule = persistDueSchedule(ScheduledReportType.TEAM_MATRIX, "PDF");

    scheduledReportService.processDueSchedules();

    ReportAttachmentDto attachment = singleSentAttachment();
    assertThat(attachment.fileName()).startsWith("TEAM_MATRIX_Marek_Manager_").endsWith(".pdf");
    assertThat(attachment.contentType()).isEqualTo("application/pdf");
    assertThat(new String(attachment.content(), 0, 5)).isEqualTo("%PDF-");
    assertThat(lastRunStatus(schedule)).isEqualTo(ScheduleRunStatus.SUCCESS);
  }

  @Test
  void dueScheduleSendsGeneratedXlsxFile() {
    ScheduledReport schedule = persistDueSchedule(ScheduledReportType.TEAM_WORKLOAD, "XLSX");

    scheduledReportService.processDueSchedules();

    ReportAttachmentDto attachment = singleSentAttachment();
    assertThat(attachment.fileName()).endsWith(".xlsx");
    assertThat(attachment.contentType())
        .isEqualTo("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
    assertThat(new String(attachment.content(), 0, 2)).isEqualTo("PK");
    assertThat(lastRunStatus(schedule)).isEqualTo(ScheduleRunStatus.SUCCESS);
  }

  private ReportAttachmentDto singleSentAttachment() {
    List<ScheduledReportEmailEvent> sent = events.stream(ScheduledReportEmailEvent.class).toList();
    assertThat(sent).hasSize(1);
    assertThat(sent.getFirst().recipientEmails()).containsExactly(manager.getEmail());
    assertThat(sent.getFirst().attachments()).hasSize(1);
    return sent.getFirst().attachments().getFirst();
  }

  private ScheduleRunStatus lastRunStatus(ScheduledReport schedule) {
    return scheduledReportRepository.findById(schedule.getScheduleId()).orElseThrow().getLastRunStatus();
  }

  private ScheduledReport persistDueSchedule(ScheduledReportType type, String format) {
    ScheduledReport schedule = new ScheduledReport();
    schedule.setName("Raport zespołu");
    schedule.setCreator(manager);
    schedule.setReportType(type);
    schedule.setReportFormat(format);
    schedule.setFrequency(ScheduleFrequency.DAILY);
    schedule.setExecutionTime(LocalTime.of(7, 0));
    schedule.setReportPeriod(ReportPeriod.PREVIOUS_WEEK);
    schedule.setRecipientEmails(manager.getEmail());
    schedule.setActive(true);
    schedule.setNextRunAt(LocalDateTime.now().minusMinutes(5));
    schedule.setTargetUsers(new HashSet<>(Set.of(manager)));
    return scheduledReportRepository.save(schedule);
  }

  private User persistUser(String name, String surname, String email, String roleName, User managerUser) {
    Role role = roleRepository.findByName(roleName).orElseGet(() -> {
      Role newRole = new Role();
      newRole.setName(roleName);
      return roleRepository.save(newRole);
    });
    User user = new User();
    user.setName(name);
    user.setSurname(surname);
    user.setEmail(email);
    user.setPasswordHash("hash");
    user.setRoles(new HashSet<>(Set.of(role)));
    user.setManager(managerUser);
    return userRepository.save(user);
  }
}
