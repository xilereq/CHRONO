package pl.chrono.timemanager.mapper;

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

}
