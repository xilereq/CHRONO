package pl.chrono.timemanager.dto.response;

import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LeaveLogDto {
  private Integer leaveLogId;
  private Integer userId;
  private Integer workLog;
  private LocalDateTime startTime;
  private LocalDateTime endTime;
}
