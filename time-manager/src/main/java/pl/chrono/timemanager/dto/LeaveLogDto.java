package pl.chrono.timemanager.dto;

import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;
import pl.chrono.dbmanager.model.enums.LeaveType;

@Getter
@Setter
public class LeaveLogDto {
  private Integer leaveLogId;
  private Integer userId;
  private LeaveType leaveType;
  private Integer workLog;
  private LocalDateTime startTime;
  private LocalDateTime endTime;
}
