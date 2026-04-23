package pl.chrono.applicationmanager.dto;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDate;

@Getter
@Setter
public class ApplicationDTO {
  private Long id;
  private String type;
  private String status;
  private LocalDate startDate;
  private LocalDate endDate;
  private String reason;
  private Integer employeeId;
}
