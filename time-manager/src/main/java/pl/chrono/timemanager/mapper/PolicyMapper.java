package pl.chrono.timemanager.mapper;

import java.time.DayOfWeek;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import pl.chrono.dbmanager.model.Policy;
import pl.chrono.timemanager.dto.request.CreatePolicyRequest;
import pl.chrono.timemanager.dto.response.PolicyDto;

@Mapper(componentModel = "spring")
public interface PolicyMapper {

  @Mapping(target = "policyId", ignore = true)
  Policy toEntity(CreatePolicyRequest request);

  @Mapping(source = "policyId", target = "id")
  PolicyDto toDto(Policy policy);

  default Set<DayOfWeek> mapWorkingDays(List<String> workingDays) {
    if (workingDays == null || workingDays.isEmpty()) {
      return new HashSet<>();
    }
    return workingDays.stream()
        .map(String::trim)
        .map(String::toUpperCase)
        .map(DayOfWeek::valueOf)
        .collect(Collectors.toSet());
  }

}
