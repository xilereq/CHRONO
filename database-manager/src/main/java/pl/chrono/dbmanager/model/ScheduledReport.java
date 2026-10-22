package pl.chrono.dbmanager.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashSet;
import java.util.Set;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.BatchSize;
import pl.chrono.dbmanager.model.enums.ReportPeriod;
import pl.chrono.dbmanager.model.enums.ScheduleFrequency;
import pl.chrono.dbmanager.model.enums.ScheduleRunStatus;
import pl.chrono.dbmanager.model.enums.ScheduledReportType;

@Entity
@Table(name = "scheduled_reports", indexes = {
    @Index(name = "idx_scheduled_reports_next_run", columnList = "is_active, next_run_at")
})
@Getter
@Setter
public class ScheduledReport {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "schedule_id")
  private Long scheduleId;

  @Column(nullable = false, length = 120)
  private String name;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "creator_id", nullable = false)
  private User creator;

  @Enumerated(EnumType.STRING)
  @Column(name = "report_type", nullable = false, length = 40)
  private ScheduledReportType reportType;

  @Column(name = "report_format", nullable = false, length = 15)
  private String reportFormat;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private ScheduleFrequency frequency;

  @Column(name = "day_of_week")
  private Integer dayOfWeek;

  @Column(name = "day_of_month")
  private Integer dayOfMonth;

  @Column(name = "execution_time", nullable = false)
  private LocalTime executionTime = LocalTime.of(7, 0);

  @Enumerated(EnumType.STRING)
  @Column(name = "report_period", nullable = false, length = 30)
  private ReportPeriod reportPeriod;

  @Column(name = "custom_start_date")
  private LocalDate customStartDate;

  @Column(name = "custom_end_date")
  private LocalDate customEndDate;

  @Column(name = "recipient_emails", nullable = false, length = 1000)
  private String recipientEmails;

  @Column(name = "is_active", nullable = false)
  private boolean isActive = true;

  @Column(name = "next_run_at", nullable = false)
  private LocalDateTime nextRunAt;

  @Column(name = "last_run_at")
  private LocalDateTime lastRunAt;

  @Enumerated(EnumType.STRING)
  @Column(name = "last_run_status", length = 20)
  private ScheduleRunStatus lastRunStatus;

  @Column(name = "retry_count", nullable = false)
  private int retryCount = 0;

  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt = LocalDateTime.now();

  @BatchSize(size = 50)
  @ManyToMany(fetch = FetchType.LAZY)
  @JoinTable(
      name = "scheduled_report_target_users",
      joinColumns = @JoinColumn(name = "schedule_id"),
      inverseJoinColumns = @JoinColumn(name = "user_id")
  )
  private Set<User> targetUsers = new HashSet<>();
}
