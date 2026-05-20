package pl.chrono.timemanager.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import pl.chrono.dbmanager.model.WorkLog;
import pl.chrono.timemanager.dto.response.WorkLogDto;

@Mapper(componentModel = "spring")
public interface WorkLogMapper {

  @Mapping(source = "user.userId", target = "userId")
  WorkLogDto toDto(WorkLog workLog);

}
