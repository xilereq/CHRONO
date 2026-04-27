package pl.chrono.dbmanager.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "operation_logs")
@Data
public class OperationLog {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  private String userEmail;
  private String action;
  private String details;
  private LocalDateTime timestamp;

}
