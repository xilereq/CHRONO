package pl.chrono.timemanager.dto.response;

import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;
import pl.chrono.dbmanager.model.enums.WorkLocationType;

@Getter
@Setter
public class WorkLogDto {
  private Integer workLogId;
  private Integer userId;
  private LocalDateTime startTime;
  private LocalDateTime endTime;
  private WorkLocationType locationType;
}
