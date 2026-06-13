package pl.chrono.timemanager.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import pl.chrono.dbmanager.model.LeaveLog;
import pl.chrono.timemanager.dto.response.LeaveLogDto;

@Mapper(componentModel = "spring")
public interface LeaveLogMapper {

  @Mapping(source = "user.userId", target = "userId")
  @Mapping(source = "workLog.workLogId", target = "workLog")
  LeaveLogDto toDto(LeaveLog leaveLog);

}
