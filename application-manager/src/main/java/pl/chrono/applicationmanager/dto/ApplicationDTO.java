package pl.chrono.applicationmanager.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ApplicationDTO {
  private Long id;
  private String type;
  private String status;
  private LocalDate startDate;
  private LocalDate endDate;
  private String reason;
  private String decisionReason;
  private Integer employeeId;
  private Integer reviewerId;
  private LocalDateTime creationDateTime;
  private LocalDateTime decisionDateTime;
}
