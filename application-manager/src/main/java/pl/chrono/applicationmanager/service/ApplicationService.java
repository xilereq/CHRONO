package pl.chrono.applicationmanager.service;

import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.chrono.applicationmanager.api.I_UserApplication;
import pl.chrono.applicationmanager.dto.ApplicationDTO;
import pl.chrono.dbmanager.model.Application;
import pl.chrono.dbmanager.model.User;
import pl.chrono.dbmanager.model.enums.ApplicationStatus;
import pl.chrono.dbmanager.model.enums.ApplicationType;
import pl.chrono.dbmanager.repository.ApplicationRepository;
import pl.chrono.dbmanager.repository.UserRepository;

@Service
@RequiredArgsConstructor
public class ApplicationService implements I_UserApplication {

  private final ApplicationRepository applicationRepository;
  private final UserRepository userRepository;

  @Override
  @Transactional
  public ApplicationDTO createApplication(ApplicationDTO request, Integer employeeId) {
    User employee = userRepository.findById(employeeId)
        .orElseThrow(() -> new RuntimeException("Nie znaleziono pracownika o ID: " + employeeId));

    Application application = new Application();
    application.setEmployee(employee);
    application.setType(ApplicationType.valueOf(request.getType()));
    application.setStartDate(request.getStartDate());
    application.setEndDate(request.getEndDate());
    application.setReason(request.getReason());
    application.setStatus(ApplicationStatus.PENDING);

    Application savedApplication = applicationRepository.save(application);
    return mapToDTO(savedApplication);
  }

  @Override
  @Transactional
  public ApplicationDTO changeStatus(Long applicationId, String newStatus, Integer reviewerId, String decisionReason) {
    Application application = applicationRepository.findById(applicationId)
        .orElseThrow(() -> new RuntimeException("Nie znaleziono wniosku o ID: " + applicationId));

    User reviewer = userRepository.findById(reviewerId)
        .orElseThrow(() -> new RuntimeException("Nie znaleziono osoby rozpatrującej o ID: "
            + reviewerId));

    application.setStatus(ApplicationStatus.valueOf(newStatus));
    application.setReviewer(reviewer);

    application.setDecisionReason(decisionReason);

    Application updatedApplication = applicationRepository.save(application);
    return mapToDTO(updatedApplication);
  }

  @Override
  public List<ApplicationDTO> getUserApplications(Integer employeeId) {
    return applicationRepository.findAllByEmployeeUserId(employeeId)
        .stream()
        .map(this::mapToDTO)
        .collect(Collectors.toList());
  }

  private ApplicationDTO mapToDTO(Application entity) {
    ApplicationDTO dto = new ApplicationDTO();
    dto.setId(entity.getId());
    dto.setType(entity.getType().name());
    dto.setStatus(entity.getStatus().name());
    dto.setStartDate(entity.getStartDate());
    dto.setEndDate(entity.getEndDate());
    dto.setReason(entity.getReason());
    dto.setDecisionReason(entity.getDecisionReason());
    dto.setEmployeeId(entity.getEmployee().getUserId());
    return dto;
  }
}
