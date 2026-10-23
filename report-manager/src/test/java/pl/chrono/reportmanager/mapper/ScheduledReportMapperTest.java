package pl.chrono.reportmanager.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import pl.chrono.dbmanager.model.ScheduledReport;
import pl.chrono.dbmanager.model.User;
import pl.chrono.dbmanager.model.enums.ReportPeriod;
import pl.chrono.dbmanager.model.enums.ScheduleFrequency;
import pl.chrono.dbmanager.model.enums.ScheduleRunStatus;
import pl.chrono.dbmanager.model.enums.ScheduledReportType;
import pl.chrono.reportmanager.dto.ReportFormat;
import pl.chrono.reportmanager.dto.ScheduledReportRequestDto;
import pl.chrono.reportmanager.dto.ScheduledReportResponseDto;

class ScheduledReportMapperTest {

  private final ScheduledReportMapper mapper = Mappers.getMapper(ScheduledReportMapper.class);

  @Test
  void toDtoFullEntityTest() {
    User creator = new User();
    creator.setUserId(10);
    creator.setName("Jan");
    creator.setSurname("Kowalski");

    User target1 = new User();
    target1.setUserId(10);
    User target2 = new User();
    target2.setUserId(20);
    User targetNullId = new User();

    ScheduledReport entity = new ScheduledReport();
    entity.setScheduleId(100L);
    entity.setName("Tygodniowy raport");
    entity.setCreator(creator);
    entity.setReportType(ScheduledReportType.TEAM_WORKLOAD);
    entity.setReportFormat("XLSX");
    entity.setFrequency(ScheduleFrequency.WEEKLY);
    entity.setDayOfWeek(1);
    entity.setExecutionTime(LocalTime.of(8, 30));
    entity.setReportPeriod(ReportPeriod.PREVIOUS_WEEK);
    entity.setRecipientEmails(" jan@chrono.pl , hr@chrono.pl , ");
    entity.setActive(true);
    entity.setNextRunAt(LocalDateTime.of(2026, 5, 4, 8, 30));
    entity.setLastRunAt(LocalDateTime.of(2026, 4, 27, 8, 30));
    entity.setLastRunStatus(ScheduleRunStatus.SUCCESS);
    entity.setRetryCount(2);
    entity.setTargetUsers(new HashSet<>(Set.of(target1, target2, targetNullId)));

    ScheduledReportResponseDto dto = mapper.toDto(entity);

    assertThat(dto).isNotNull();
    assertThat(dto.scheduleId()).isEqualTo(100L);
    assertThat(dto.name()).isEqualTo("Tygodniowy raport");
    assertThat(dto.creatorId()).isEqualTo(10);
    assertThat(dto.creatorFullName()).isEqualTo("Jan Kowalski");
    assertThat(dto.reportType()).isEqualTo(ScheduledReportType.TEAM_WORKLOAD);
    assertThat(dto.reportFormat()).isEqualTo(ReportFormat.XLSX);
    assertThat(dto.frequency()).isEqualTo(ScheduleFrequency.WEEKLY);
    assertThat(dto.dayOfWeek()).isEqualTo(1);
    assertThat(dto.executionTime()).isEqualTo(LocalTime.of(8, 30));
    assertThat(dto.reportPeriod()).isEqualTo(ReportPeriod.PREVIOUS_WEEK);
    assertThat(dto.targetUserIds()).containsExactlyInAnyOrder(10, 20);
    assertThat(dto.recipientEmails()).containsExactly("jan@chrono.pl", "hr@chrono.pl");
    assertThat(dto.isActive()).isTrue();
    assertThat(dto.lastRunStatus()).isEqualTo(ScheduleRunStatus.SUCCESS);
    assertThat(dto.retryCount()).isEqualTo(2);
  }

  @Test
  void toDtoNullFieldsTest() {
    assertThat(mapper.toDto(null)).isNull();

    ScheduledReport entity = new ScheduledReport();
    entity.setActive(false);
    entity.setReportFormat(null);
    entity.setRecipientEmails("   ");
    entity.setTargetUsers(null);

    ScheduledReportResponseDto dto = mapper.toDto(entity);

    assertThat(dto.creatorId()).isNull();
    assertThat(dto.creatorFullName()).isNull();
    assertThat(dto.reportFormat()).isNull();
    assertThat(dto.targetUserIds()).isEmpty();
    assertThat(dto.recipientEmails()).isEmpty();
    assertThat(dto.isActive()).isFalse();
  }

  @Test
  void updateEntityFromDtoDefaultExecutionTimeAndTrimEmailsTest() {
    ScheduledReportRequestDto request = new ScheduledReportRequestDto(
        "Raport miesięczny",
        ScheduledReportType.EMPLOYEE_OVERTIME,
        ReportFormat.CSV,
        ScheduleFrequency.MONTHLY,
        null,
        15,
        null,
        ReportPeriod.CUSTOM,
        LocalDate.of(2026, 1, 1),
        LocalDate.of(2026, 1, 31),
        List.of(1, 2),
        Arrays.asList(" a@chrono.pl ", null, "  ", "b@chrono.pl")
    );

    ScheduledReport entity = new ScheduledReport();
    entity.setScheduleId(55L);
    entity.setActive(false);
    entity.setRetryCount(4);

    mapper.updateEntityFromDto(request, entity);

    assertThat(entity.getScheduleId()).isEqualTo(55L);
    assertThat(entity.isActive()).isFalse();
    assertThat(entity.getRetryCount()).isEqualTo(4);
    assertThat(entity.getName()).isEqualTo("Raport miesięczny");
    assertThat(entity.getReportType()).isEqualTo(ScheduledReportType.EMPLOYEE_OVERTIME);
    assertThat(entity.getReportFormat()).isEqualTo("CSV");
    assertThat(entity.getFrequency()).isEqualTo(ScheduleFrequency.MONTHLY);
    assertThat(entity.getDayOfMonth()).isEqualTo(15);
    assertThat(entity.getExecutionTime()).isEqualTo(LocalTime.of(7, 0));
    assertThat(entity.getReportPeriod()).isEqualTo(ReportPeriod.CUSTOM);
    assertThat(entity.getCustomStartDate()).isEqualTo(LocalDate.of(2026, 1, 1));
    assertThat(entity.getCustomEndDate()).isEqualTo(LocalDate.of(2026, 1, 31));
    assertThat(entity.getRecipientEmails()).isEqualTo("a@chrono.pl,b@chrono.pl");
  }

  @Test
  void updateEntityFromDtoExplicitExecutionTimeAndHelperMethodsEdgeCasesTest() {
    mapper.updateEntityFromDto(null, new ScheduledReport());

    ScheduledReportRequestDto request = new ScheduledReportRequestDto(
        "Raport dzienny",
        ScheduledReportType.EMPLOYEE_TIMESHEET,
        null,
        ScheduleFrequency.DAILY,
        null,
        null,
        LocalTime.of(9, 15),
        ReportPeriod.PREVIOUS_DAY,
        null,
        null,
        Collections.emptyList(),
        Collections.emptyList()
    );

    ScheduledReport entity = new ScheduledReport();
    mapper.updateEntityFromDto(request, entity);

    assertThat(entity.getExecutionTime()).isEqualTo(LocalTime.of(9, 15));
    assertThat(entity.getReportFormat()).isNull();
    assertThat(entity.getRecipientEmails()).isEmpty();
    assertThat(mapper.usersToIds(Collections.emptySet())).isEmpty();
    assertThat(mapper.stringToEmails(null)).isEmpty();
    assertThat(mapper.emailsToString(null)).isEmpty();
  }
}
