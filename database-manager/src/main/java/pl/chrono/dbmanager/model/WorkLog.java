package pl.chrono.dbmanager.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "work_log")
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
    return this.getLeaveLogs().stream()
        .mapToLong(leave -> {
          LocalDateTime leaveStart = leave.getStartTime();
          LocalDateTime leaveEnd = leave.getEndTime() != null ? leave.getEndTime() : boundaryTime;

          if (leaveStart.isAfter(leaveEnd)) {
            return 0L;
          }
          return Duration.between(leaveStart, leaveEnd).getSeconds();
        })
        .sum();
  }

  public int getRequiredSeconds() {
    if (this.user != null && this.user.getActivePolicy() != null && this.startTime != null) {
      return this.user.getActivePolicy().getRequiredSecondsForDate(this.startTime.toLocalDate());
    }
    return 8 * 3600;
  }

  public int getMaxAllowedSeconds() {
    if (this.user != null && this.user.getActivePolicy() != null) {
      return this.user.getActivePolicy().getMaxAllowedSeconds();
    }
    return 12 * 3600;
  }

  public int getRoundingSeconds() {
    if (this.user != null && this.user.getActivePolicy() != null
        && this.user.getActivePolicy().getRoundingMinutes() != null) {
      return this.user.getActivePolicy().getRoundingMinutes() * 60;
    }
    return 0;
  }

  public void forceClose(LocalDateTime boundaryTime) {
    if (this.getStartTime() == null) {
      return;
    }

    int requiredSeconds = getRequiredSeconds();

    LocalDateTime autoCloseTime = this.startTime
        .plusSeconds(requiredSeconds)
        .plusSeconds(calculateBreakTime(boundaryTime));
    if (this.leaveLogs != null) {
      this.leaveLogs.stream().filter(
          leave -> leave.getEndTime() == null)
          .forEach(leave -> leave.setEndTime(autoCloseTime));
    }
    this.endTime = autoCloseTime;
  }

  public long getEffectiveWorkTime(LocalDateTime boundaryTime) {
    long exactWorkTime = calculateWorkTime(boundaryTime);
    int requiredSeconds = getRequiredSeconds();
    int roundingSeconds = getRoundingSeconds();
    if (exactWorkTime >= (requiredSeconds - roundingSeconds) && exactWorkTime < requiredSeconds) {
      return requiredSeconds;
    }
    return exactWorkTime;
  }

}
