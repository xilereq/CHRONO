package pl.chrono.timemanager.dto;

import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Getter
@Setter
public class WorkLogDto {
  private Integer workLogId;
  private Integer userId;
  private LocalDateTime startTime;
  private LocalDateTime endTime;
}
