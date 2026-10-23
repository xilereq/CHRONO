package pl.chrono.reportmanager.mapper;

import java.time.LocalTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;
import pl.chrono.dbmanager.model.ScheduledReport;
import pl.chrono.dbmanager.model.User;
import pl.chrono.reportmanager.dto.ReportFormat;
import pl.chrono.reportmanager.dto.ScheduledReportRequestDto;
import pl.chrono.reportmanager.dto.ScheduledReportResponseDto;

@Mapper(componentModel = "spring")
public interface ScheduledReportMapper {

  @Mapping(source = "creator.userId", target = "creatorId")
  @Mapping(
      expression = "java(entity.getCreator() != null ? entity.getCreator().getName() + \" \" + entity.getCreator().getSurname() : null)",
      target = "creatorFullName"
  )
  @Mapping(source = "reportFormat", target = "reportFormat", qualifiedByName = "stringToReportFormat")
  @Mapping(source = "targetUsers", target = "targetUserIds", qualifiedByName = "usersToIds")
  @Mapping(source = "recipientEmails", target = "recipientEmails", qualifiedByName = "stringToEmails")
  @Mapping(source = "active", target = "isActive")
  ScheduledReportResponseDto toDto(ScheduledReport entity);

  @Mapping(target = "scheduleId", ignore = true)
  @Mapping(target = "creator", ignore = true)
  @Mapping(target = "active", ignore = true)
  @Mapping(target = "nextRunAt", ignore = true)
  @Mapping(target = "lastRunAt", ignore = true)
  @Mapping(target = "lastRunStatus", ignore = true)
  @Mapping(target = "retryCount", ignore = true)
  @Mapping(target = "createdAt", ignore = true)
  @Mapping(target = "targetUsers", ignore = true)
  @Mapping(source = "reportFormat", target = "reportFormat", qualifiedByName = "reportFormatToString")
  @Mapping(source = "recipientEmails", target = "recipientEmails", qualifiedByName = "emailsToString")
  @Mapping(source = "executionTime", target = "executionTime", qualifiedByName = "resolveExecutionTime")
  void updateEntityFromDto(ScheduledReportRequestDto dto, @MappingTarget ScheduledReport entity);

  @Named("stringToReportFormat")
  default ReportFormat stringToReportFormat(String format) {
    return format != null ? ReportFormat.valueOf(format) : null;
  }

  @Named("reportFormatToString")
  default String reportFormatToString(ReportFormat format) {
    return format != null ? format.name() : null;
  }

  @Named("usersToIds")
  default List<Integer> usersToIds(Set<User> users) {
    if (users == null || users.isEmpty()) {
      return Collections.emptyList();
    }
    return users.stream()
        .map(User::getUserId)
        .filter(Objects::nonNull)
        .toList();
  }

  @Named("stringToEmails")
  default List<String> stringToEmails(String emails) {
    if (emails == null || emails.isBlank()) {
      return Collections.emptyList();
    }
    return Arrays.stream(emails.split(","))
        .map(String::trim)
        .filter(s -> !s.isEmpty())
        .toList();
  }

  @Named("emailsToString")
  default String emailsToString(List<String> emails) {
    if (emails == null || emails.isEmpty()) {
      return "";
    }
    return emails.stream()
        .filter(Objects::nonNull)
        .map(String::trim)
        .filter(s -> !s.isEmpty())
        .collect(Collectors.joining(","));
  }

  @Named("resolveExecutionTime")
  default LocalTime resolveExecutionTime(LocalTime executionTime) {
    return executionTime != null ? executionTime : LocalTime.of(7, 0);
  }
}
