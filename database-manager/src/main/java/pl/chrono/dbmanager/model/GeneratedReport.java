package pl.chrono.dbmanager.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;
import pl.chrono.dbmanager.model.enums.ReportType;

@Entity
@Table(name = "generated_reports", indexes = {
    @Index(name = "idx_genreport_subject_created", columnList = "subject_user_id, created_at")
})
@Getter
@Setter
public class GeneratedReport {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Enumerated(EnumType.STRING)
  @Column(name = "report_type", nullable = false, length = 50)
  private ReportType reportType;

  @Column(name = "format", nullable = false, length = 10)
  private String format;

  @Column(name = "period_start", nullable = false)
  private LocalDate periodStart;

  @Column(name = "period_end", nullable = false)
  private LocalDate periodEnd;

  // pracownik (raporty indywidualne) lub menedżer (raporty zespołowe)
  @Column(name = "subject_user_id")
  private Integer subjectUserId;

  // null, gdy raport wygenerował harmonogram
  @Column(name = "requested_by_user_id")
  private Integer requestedByUserId;

  @Column(name = "file_name", nullable = false)
  private String fileName;

  @Column(name = "content_type", nullable = false, length = 100)
  private String contentType;

  @Column(name = "file_size", nullable = false)
  private long fileSize;

  @Column(name = "content", nullable = false)
  private byte[] content;

  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt = LocalDateTime.now();
}
