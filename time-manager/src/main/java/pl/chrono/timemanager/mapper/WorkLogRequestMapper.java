package pl.chrono.timemanager.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import pl.chrono.dbmanager.model.WorkLogRequest;
import pl.chrono.timemanager.dto.response.WorkLogRequestDto;

@Mapper(componentModel = "spring")
public interface WorkLogRequestMapper {

  @Mapping(source = "employee.userId", target = "employeeId")
  @Mapping(expression = "java(entity.getEmployee() != null ? entity.getEmployee().getName() + \" " +
      "\" + entity.getEmployee().getSurname() : null)", target = "employeeName")
  @Mapping(source = "manager.userId", target = "managerId")
  @Mapping(expression = "java(entity.getManager() != null ? entity.getManager().getName() + \" \"" +
      " + entity.getManager().getSurname() : null)", target = "managerName")
  @Mapping(source = "targetWorkLog.workLogId", target = "targetWorkLogId")
  WorkLogRequestDto toDto(WorkLogRequest entity);

}
