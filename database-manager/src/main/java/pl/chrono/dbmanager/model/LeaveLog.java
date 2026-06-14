package pl.chrono.dbmanager.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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

@Entity
@Table(name = "leave_log", indexes = {
    @Index(name = "idx_leave_worklog", columnList = "work_log_id"),
    @Index(name = "idx_leave_user_time", columnList = "user_id, start_time")
})
@Getter
@Setter
public class LeaveLog {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "leave_log_id")
  private Integer leaveLogId;

  @ManyToOne
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  @ManyToOne
  @JoinColumn(name = "work_log_id")
  private WorkLog workLog;

  private LocalDateTime startTime;
  private LocalDateTime endTime;
}
