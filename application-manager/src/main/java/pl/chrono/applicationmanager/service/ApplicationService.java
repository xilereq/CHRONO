package pl.chrono.applicationmanager.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.context.ApplicationEventPublisher;
import pl.chrono.applicationmanager.api.I_UserApplication;
import pl.chrono.applicationmanager.event.ApplicationNotificationEvent;
import pl.chrono.applicationmanager.dto.request.CreateApplicationRequest;
import pl.chrono.applicationmanager.dto.response.ApplicationResponse;
import pl.chrono.dbmanager.model.Application;
import pl.chrono.dbmanager.model.User;
import pl.chrono.dbmanager.model.enums.ApplicationStatus;
import pl.chrono.dbmanager.model.enums.ApplicationType;
import pl.chrono.dbmanager.repository.ApplicationRepository;
import pl.chrono.dbmanager.repository.UserRepository;
import pl.chrono.logmanager.audit.LogActivity;
import pl.chrono.timemanager.service.OvertimeService;


@Service
@RequiredArgsConstructor
public class ApplicationService implements I_UserApplication {

  private final ApplicationRepository applicationRepository;
  private final UserRepository userRepository;
  private final ApplicationEventPublisher eventPublisher;

  private final OvertimeService overtimeService;

  @Override
  @Transactional
  @LogActivity(action = "CREATE_APPLICATION", details = "The employee submitted a new application")
  public ApplicationResponse createApplication(CreateApplicationRequest request, Integer employeeId) {
    User employee = userRepository.findById(employeeId)
        .orElseThrow(() -> new RuntimeException("Nie znaleziono pracownika o ID: " + employeeId));

    Application application = new Application();
    application.setEmployee(employee);
    application.setType(ApplicationType.valueOf(request.type()));
    application.setStartDate(request.startDate());
    application.setEndDate(request.endDate());
    application.setReason(request.reason());
    application.setStatus(ApplicationStatus.PENDING);
    application.setCreationDateTime(LocalDateTime.now());
    application.setDurationSeconds(request.durationSeconds());

    if (employee.getManager() != null) {
      application.setReviewer(employee.getManager());
    }

    Application savedApplication = applicationRepository.save(application);

    if (savedApplication.getType() == ApplicationType.OVERTIME_TAKE) {
      overtimeService.reserveOvertimeForApplication(savedApplication);
    }

    Integer managerId = employee.getManager() != null ? employee.getManager().getUserId() : null;
    eventPublisher.publishEvent(new ApplicationNotificationEvent(
        savedApplication.getId(),
        employee.getUserId(),
        managerId,
        "CREATED",
        "PENDING",
        null
    ));

    return mapToResponse(savedApplication);
  }

  @Override
  public List<ApplicationResponse> getAllApplications() {
    return applicationRepository.findAll()
        .stream()
        .map(this::mapToResponse)
        .toList();
  }

  @Override
  @Transactional
  @LogActivity(action = "CHANGE_APPLICATION_STATUS", details = "Changing the status of an employee's application")
  public ApplicationResponse changeStatus(Long applicationId, String newStatus, Integer reviewerId, String decisionReason) {
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

    ApplicationStatus updatedStatus = ApplicationStatus.valueOf(newStatus);
    application.setStatus(updatedStatus);
    application.setReviewer(reviewer);
    application.setDecisionReason(decisionReason);
    application.setDecisionDateTime(LocalDateTime.now());

    Application updatedApplication = applicationRepository.save(application);

    if (updatedApplication.getType() == ApplicationType.OVERTIME_TAKE
        && updatedStatus == ApplicationStatus.REJECTED) {
      overtimeService.refundOvertimeForApplication(updatedApplication);
    }

    eventPublisher.publishEvent(new ApplicationNotificationEvent(
        updatedApplication.getId(),
        employee.getUserId(),
        employee.getManager().getUserId(),
        "STATUS_CHANGED",
        newStatus,
        reviewer.getName() + " " + reviewer.getSurname()
    ));

    return mapToResponse(updatedApplication);
  }

  @Override
  public List<ApplicationResponse> getUserApplications(Integer employeeId, Integer requesterId) {
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
        .map(this::mapToResponse)
        .toList();
  }

  @Override
  public List<ApplicationResponse> getPendingApplicationsForManager(Integer managerId) {
    return applicationRepository.findPendingByManagerId(ApplicationStatus.PENDING, managerId)
        .stream()
        .map(this::mapToResponse)
        .toList();
  }

  @Override
  public List<ApplicationResponse> getAllApplicationsForManager(Integer managerId) {
    return applicationRepository.findAllByManagerId(managerId)
        .stream()
        .map(this::mapToResponse)
        .collect(Collectors.toList());
  }

  private ApplicationResponse mapToResponse(Application application) {
    return new ApplicationResponse(
        application.getId(),
        application.getType().name(),
        application.getStatus().name(),
        application.getStartDate(),
        application.getEndDate(),
        application.getDurationSeconds(),
        application.getReason(),
        application.getCreationDateTime(),
        application.getDecisionDateTime(),
        application.getDecisionReason(),
        application.getEmployee().getUserId(),
        application.getEmployee().getEmail(),
        application.getReviewer() != null ? application.getReviewer().getUserId() : null,
        application.getReviewer() != null ? application.getReviewer().getEmail() : null
    );
  }

  @Override
  @Transactional
  @LogActivity(action = "CANCEL_APPLICATION", details = "The employee canceled his own application")
  public ApplicationResponse cancelApplication(Long applicationId, Integer userId) {
    Application application = applicationRepository.findById(applicationId)
        .orElseThrow(() -> new RuntimeException("Nie znaleziono wniosku o ID: " + applicationId));

    if (!application.getEmployee().getUserId().equals(userId)) {
      throw new SecurityException("Możesz anulować tylko swoje własne wnioski.");
    }
    if (application.getStatus() != ApplicationStatus.PENDING) {
      throw new IllegalStateException("Można anulować tylko wniosek w statusie PENDING.");
    }
    if (application.getType() == ApplicationType.OVERTIME_TAKE) {
      overtimeService.refundOvertimeForApplication(application);
    }

    application.setStatus(ApplicationStatus.CANCELLED);
    application.setDecisionDateTime(LocalDateTime.now());
    Application saved = applicationRepository.save(application);
    return mapToResponse(saved);
  }
}
