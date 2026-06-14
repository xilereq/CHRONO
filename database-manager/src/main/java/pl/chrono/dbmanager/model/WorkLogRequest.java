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
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;
import pl.chrono.dbmanager.model.enums.WorkLogRequestStatus;

@Entity
@Table(name = "work_log_request", indexes = {
    @Index(name = "idx_wlr_employee", columnList = "employee_id"),
    @Index(name = "idx_wlr_manager_status", columnList = "manager_id, status")
})
@Getter
@Setter
public class WorkLogRequest {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "employee_id", nullable = false)
  private User employee;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "manager_id")
  private User manager;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private WorkLogRequestStatus status = WorkLogRequestStatus.PENDING;

  @Column(name = "proposed_start_time", nullable = false)
  private LocalDateTime proposedStartTime;

  @Column(name = "proposed_end_time", nullable = false)
  private LocalDateTime proposedEndTime;

  @Column(length = 500)
  private String reason;

  @Column(name = "manager_comment", length = 500)
  private String managerComment;

  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt = LocalDateTime.now();

  @Column(name = "resolved_at")
  private LocalDateTime resolvedAt;
}
