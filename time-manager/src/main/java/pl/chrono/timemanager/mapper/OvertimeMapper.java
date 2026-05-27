package pl.chrono.timemanager.mapper;

import org.mapstruct.Mapper;
import pl.chrono.dbmanager.model.OvertimeArchive;
import pl.chrono.timemanager.dto.response.OvertimeRecordDto;

@Mapper(componentModel = "spring")
public interface OvertimeMapper {
  OvertimeRecordDto toDto(OvertimeArchive archive);
}
