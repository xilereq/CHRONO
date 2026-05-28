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
import pl.chrono.dbmanager.model.enums.OvertimeType;

@Entity
@Table(name = "overtime_archive", indexes = {
    @Index(name = "idx_overtime_user_id", columnList = "user_id"),
    @Index(name = "idx_overtime_valid_until", columnList = "valid_until, type")
})
@Getter
@Setter
public class OvertimeArchive {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private OvertimeType type;

  @Column(nullable = false)
  private Integer durationSeconds;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "application_id")
  private Application application;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "work_log_id")
  private WorkLog workLog;

  @Column(nullable = false, updatable = false)
  private LocalDateTime createdAt = LocalDateTime.now();

  @Column(nullable = false)
  private LocalDateTime validUntil;

  @Column(length = 500)
  private String note;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "operator_id")
  private User operator;
}
