package pl.chrono.dbmanager.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import pl.chrono.dbmanager.model.enums.ApplicationType;
import pl.chrono.dbmanager.model.enums.ApplicationStatus;
import java.time.LocalDate;

@Entity
@Table(name = "application")
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
}
