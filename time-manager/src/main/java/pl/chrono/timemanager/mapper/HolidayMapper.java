package pl.chrono.timemanager.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import pl.chrono.dbmanager.model.Holiday;
import pl.chrono.timemanager.dto.request.CreateHolidayRequest;
import pl.chrono.timemanager.dto.response.HolidayDto;

@Mapper(componentModel = "spring")
public interface HolidayMapper {

  @Mapping(target = "holidayId", ignore = true)
  Holiday toEntity(CreateHolidayRequest request);

  HolidayDto toDto(Holiday holiday);

}
