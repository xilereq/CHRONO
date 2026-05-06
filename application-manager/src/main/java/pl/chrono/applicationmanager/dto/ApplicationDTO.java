package pl.chrono.applicationmanager.dto;

import java.time.LocalDate;
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
}
