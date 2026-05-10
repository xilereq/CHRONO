package pl.chrono.applicationmanager.service;

import java.time.LocalDateTime;
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
import pl.chrono.logmanager.audit.LogActivity;

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
    application.setCreationDateTime(LocalDateTime.now());

    Application savedApplication = applicationRepository.save(application);
    return mapToDTO(savedApplication);
  }

  @Override
  @Transactional
  @LogActivity(action = "zmiana")
  public ApplicationDTO changeStatus(Long applicationId, String newStatus, Integer reviewerId, String decisionReason) {
    Application application = applicationRepository.findById(applicationId)
        .orElseThrow(() -> new RuntimeException("Nie znaleziono wniosku o ID: " + applicationId));

    if (application.getStatus() != ApplicationStatus.PENDING) {
      throw new IllegalStateException("Wniosek został już rozpatrzony (status: "
          + application.getStatus() + ") i nie można zmienić jego statusu.");
    }

    User employee = application.getEmployee();

    if (employee.getManager() == null || !employee.getManager().getUserId().equals(reviewerId)) {
      throw new SecurityException("Brak uprawnień. Tylko bezpośredni przełożony może rozpatrywać wnioski tego pracownika.");
    }

    User reviewer = userRepository.findById(reviewerId)
        .orElseThrow(() -> new RuntimeException("Nie znaleziono osoby rozpatrującej o ID: "
            + reviewerId));

    application.setStatus(ApplicationStatus.valueOf(newStatus));
    application.setReviewer(reviewer);
    application.setDecisionReason(decisionReason);
    application.setDecisionDateTime(LocalDateTime.now());

    Application updatedApplication = applicationRepository.save(application);
    return mapToDTO(updatedApplication);
  }

  @Override
  public List<ApplicationDTO> getUserApplications(Integer employeeId, Integer requesterId) {
    if (!employeeId.equals(requesterId)) {
      User employee = userRepository.findById(employeeId)
          .orElseThrow(() -> new RuntimeException("Nie znaleziono pracownika o ID: " + employeeId));
      User requester = userRepository.findById(requesterId)
          .orElseThrow(() -> new RuntimeException("Nie znaleziono użytkownika żądającego o ID: " + requesterId));

      boolean hasGlobalAccess = requester.getRoles().stream()
          .anyMatch(role -> role.getName().equals("ROLE_HR") || role.getName().equals("ROLE_ADMIN"));

      boolean isDirectManager = employee.getManager() != null
          && employee.getManager().getUserId().equals(requesterId);

      if (!hasGlobalAccess && !isDirectManager) {
        throw new SecurityException("Brak uprawnień. Możesz przeglądać tylko swoje wnioski lub wnioski swoich podwładnych.");
      }
    }

    return applicationRepository.findAllByEmployeeUserId(employeeId)
        .stream()
        .map(this::mapToDTO)
        .collect(Collectors.toList());
  }

  @Override
  public List<ApplicationDTO> getPendingApplicationsForManager(Integer managerId) {
    return applicationRepository.findPendingByManagerId(ApplicationStatus.PENDING, managerId)
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
    dto.setCreationDateTime(entity.getCreationDateTime());
    dto.setDecisionDateTime(entity.getDecisionDateTime());

    if (entity.getReviewer() != null) {
      dto.setReviewerId(entity.getReviewer().getUserId());
    }

    return dto;
  }
}
