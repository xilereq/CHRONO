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
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;
import pl.chrono.dbmanager.model.enums.ApplicationStatus;
import pl.chrono.dbmanager.model.enums.ApplicationType;

@Entity
@Table(name = "application", indexes = {
    @Index(name = "idx_app_emp_date_status", columnList = "employee_id, start_date, status"),
    @Index(name = "idx_app_employee_id", columnList = "employee_id")
})
@Getter
@Setter
public class Application {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "employee_id", nullable = false)
  private User employee;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "reviewer_id")
  private User reviewer;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private ApplicationType type;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private ApplicationStatus status = ApplicationStatus.PENDING;

  @Column(name = "start_date", nullable = false)
  private LocalDate startDate;

  @Column(name = "end_date", nullable = false)
  private LocalDate endDate;

  @Column(length = 500)
  private String reason;

  @Column(name = "decision_reason")
  private String decisionReason;

  @Column(name = "creation_date_time", updatable = false)
  private LocalDateTime creationDateTime;

  @Column(name = "decision_date_time")
  private LocalDateTime decisionDateTime;

  @Column(name = "duration_seconds")
  private Integer durationSeconds;
}
