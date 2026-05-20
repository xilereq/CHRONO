package pl.chrono.timemanager.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.chrono.dbmanager.model.Policy;
import pl.chrono.dbmanager.repository.PolicyRepository;
import pl.chrono.timemanager.dto.request.CreatePolicyRequest;
import pl.chrono.timemanager.dto.response.PolicyDto;
import pl.chrono.timemanager.mapper.PolicyMapper;

@Service
@RequiredArgsConstructor
public class PolicyConfigurationService {
  private final PolicyRepository policyRepository;
  private final PolicyMapper policyMapper;

  @Transactional
  public PolicyDto createPolicy(CreatePolicyRequest request) {
    Policy policy = policyMapper.toEntity(request);

    Policy saved = policyRepository.save(policy);

    return policyMapper.toDto(saved);

  }
}
