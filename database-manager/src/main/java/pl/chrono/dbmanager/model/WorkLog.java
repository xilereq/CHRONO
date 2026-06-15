package pl.chrono.dbmanager.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import lombok.Getter;
import lombok.Setter;
import pl.chrono.dbmanager.model.enums.WorkLocationType;

@Entity
@Table(name = "work_log", indexes = {
    @Index(name = "idx_worklog_user_time", columnList = "user_id, start_time"),
    @Index(name = "idx_worklog_settled", columnList = "is_settled")
})
@Getter
@Setter
public class WorkLog {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "work_log_id")
  private Integer workLogId;

  @ManyToOne
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  private LocalDateTime startTime;
  private LocalDateTime endTime;

  @OneToMany(mappedBy = "workLog", orphanRemoval = true, cascade = CascadeType.ALL)
  private java.util.List<LeaveLog> leaveLogs = new ArrayList<>();

  @Column(name = "is_settled", nullable = false, columnDefinition = "BOOLEAN DEFAULT FALSE")
  private boolean settled;

  @Enumerated(EnumType.STRING)
  @Column(name = "location_type", nullable = false)
  private WorkLocationType locationType = WorkLocationType.OFFICE;

  public long calculateWorkTime(LocalDateTime boundaryTime) {
    LocalDateTime endTime = this.getEndTime() != null ? this.getEndTime() : boundaryTime;
    if (this.startTime == null || this.getStartTime().isAfter(endTime)) {
      return 0L;
    }
    long workTime = Duration.between(this.getStartTime(), endTime).getSeconds();
    long breakTime = calculateBreakTime(endTime);
    return Math.max(0L, workTime - breakTime);
  }

  public long calculateBreakTime(LocalDateTime boundaryTime) {
    if (this.getLeaveLogs() == null || this.getLeaveLogs().isEmpty()) {
      return 0L;
    }
    return this.getLeaveLogs().stream().mapToLong(leave -> {
      LocalDateTime leaveStart = leave.getStartTime();
      LocalDateTime leaveEnd = leave.getEndTime() != null ? leave.getEndTime() : boundaryTime;
      return leaveStart.isAfter(leaveEnd) ? 0L : Duration.between(leaveStart, leaveEnd)
          .getSeconds();
    }).sum();
  }

  private long calculateClosedBreakTime() {
    if (this.getLeaveLogs() == null || this.getLeaveLogs().isEmpty()) {
      return 0L;
    }
    return this.getLeaveLogs().stream().filter(leave -> leave.getEndTime() != null)
        .mapToLong(leave -> Duration.between(leave.getStartTime(), leave.getEndTime()).getSeconds())
        .sum();
  }


  public void forceClose(LocalDateTime boundaryTime, int targetSeconds) {
    if (this.getStartTime() == null) {
      return;
    }
    LocalDateTime baseEndTime = this.startTime.plusSeconds(targetSeconds);
    LocalDateTime autoCloseTime = baseEndTime.plusSeconds(calculateClosedBreakTime());
    if (autoCloseTime.isAfter(boundaryTime)) {
      autoCloseTime = boundaryTime;
    }
    if (this.leaveLogs != null) {
      LocalDateTime finalAutoCloseTime = autoCloseTime;
      this.leaveLogs.stream().filter(
          leave -> leave.getEndTime() == null)
          .forEach(leave -> leave.setEndTime(finalAutoCloseTime));
    }
    this.endTime = autoCloseTime;
  }

  public long getEffectiveWorkTime(LocalDateTime boundaryTime, int requiredSeconds,
                                   int roundingSeconds) {
    long exactWorkTime = calculateWorkTime(boundaryTime);
    if (exactWorkTime >= (requiredSeconds - roundingSeconds) && exactWorkTime < requiredSeconds) {
      return requiredSeconds;
    }
    return exactWorkTime;
  }
}
