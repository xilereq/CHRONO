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
import pl.chrono.applicationmanager.exception.ApplicationException;
import pl.chrono.applicationmanager.exception.ApplicationSecurityException;
import pl.chrono.applicationmanager.exception.ApplicationUserException;
import pl.chrono.dbmanager.model.Application;
import pl.chrono.dbmanager.model.User;
import pl.chrono.dbmanager.model.enums.ApplicationStatus;
import pl.chrono.dbmanager.model.enums.ApplicationType;
import pl.chrono.dbmanager.repository.ApplicationRepository;
import pl.chrono.dbmanager.repository.UserRepository;
import pl.chrono.logmanager.audit.LogActivity;
import pl.chrono.timemanager.service.OvertimeService;
import pl.chrono.timemanager.service.ApplicationValidationService;


@Service
@RequiredArgsConstructor
public class ApplicationService implements I_UserApplication {

  private final ApplicationRepository applicationRepository;
  private final UserRepository userRepository;
  private final ApplicationEventPublisher eventPublisher;

  private final OvertimeService overtimeService;
  private final ApplicationValidationService applicationValidationService;

  @Override
  @Transactional
  @LogActivity(action = "CREATE_APPLICATION", details = "The employee submitted a new application")
  public ApplicationResponse createApplication(CreateApplicationRequest request, Integer employeeId) {
    User employee = userRepository.findById(employeeId)
        .orElseThrow(() -> new ApplicationUserException.NotFound(employeeId));

    return internalCreateApplication(request, employee, ApplicationStatus.PENDING);
  }

  @Override
  @Transactional
  @LogActivity(action = "CREATE_APPLICATION_FOR_EMPLOYEE", details = "A manager submitted a new application for an employee")
  public ApplicationResponse createApplicationForEmployee(CreateApplicationRequest request, Integer targetEmployeeId, Integer managerId) {
    User manager = userRepository.findById(managerId)
        .orElseThrow(() -> new ApplicationUserException.NotFound(managerId));
    User employee = userRepository.findById(targetEmployeeId)
        .orElseThrow(() -> new ApplicationUserException.NotFound(targetEmployeeId));

    boolean hasGlobalAccess = manager.getRoles().stream()
        .anyMatch(role -> role.getName().equals("ROLE_HR") || role.getName().equals("ROLE_ADMIN"));

    boolean isDirectManager = employee.getManager() != null
        && employee.getManager().getUserId().equals(managerId);

    if (!hasGlobalAccess && !isDirectManager) {
      throw new ApplicationSecurityException.UnauthorizedCreation();
    }

    ApplicationResponse response = internalCreateApplication(request, employee, ApplicationStatus.PENDING);

    return changeStatus(response.id(), ApplicationStatus.APPROVED.name(), managerId, "Application submitted and approved automatically on behalf of the employee", true);
  }

  private ApplicationResponse internalCreateApplication(CreateApplicationRequest request, User employee, ApplicationStatus initialStatus) {
    ApplicationType type = ApplicationType.valueOf(request.type());

    applicationValidationService.validateApplicationLimits(
        employee.getUserId(),
        request.startDate(),
        request.endDate(),
        type,
        request.durationSeconds()
    );

    Application application = new Application();
    application.setEmployee(employee);
    application.setType(type);
    application.setStartDate(request.startDate());
    application.setEndDate(request.endDate());
    application.setReason(request.reason());
    application.setStatus(initialStatus);
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
        initialStatus.name(),
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
  public ApplicationResponse changeStatus(Long applicationId, String newStatus, Integer reviewerId, String decisionReason, Boolean isAdmin) {
    Application application = getApplicationAndValidatePending(applicationId);
    User employee = application.getEmployee();

    if (!isAdmin && (employee.getManager() == null || !employee.getManager().getUserId().equals(reviewerId))) {
        throw new ApplicationSecurityException.UnauthorizedManager();
    }

    return processStatusChange(application, newStatus, reviewerId, decisionReason);
  }

  private Application getApplicationAndValidatePending(Long applicationId) {
      Application application = applicationRepository.findById(applicationId)
          .orElseThrow(() -> new ApplicationException.NotFound(applicationId));

      if (application.getStatus() != ApplicationStatus.PENDING) {
        throw new ApplicationException.AlreadyProcessed(application.getStatus().name());
      }

      return application;
  }

  private ApplicationResponse processStatusChange(Application application, String newStatus, Integer reviewerId, String decisionReason) {
      User reviewer = userRepository.findById(reviewerId)
          .orElseThrow(() -> new ApplicationUserException.NotFound(reviewerId));

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

      Integer managerId = application.getEmployee().getManager() != null ?
          application.getEmployee().getManager().getUserId() : null;

      eventPublisher.publishEvent(new ApplicationNotificationEvent(
          updatedApplication.getId(),
          application.getEmployee().getUserId(),
          managerId,
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
          .orElseThrow(() -> new ApplicationUserException.NotFound(employeeId));
      User requester = userRepository.findById(requesterId)
          .orElseThrow(() -> new ApplicationUserException.NotFound(requesterId));

      boolean hasGlobalAccess = requester.getRoles().stream()
          .anyMatch(role -> role.getName().equals("ROLE_HR") || role.getName().equals("ROLE_ADMIN"));

      boolean isDirectManager = employee.getManager() != null
          && employee.getManager().getUserId().equals(requesterId);

      if (!hasGlobalAccess && !isDirectManager) {
        throw new ApplicationSecurityException.UnauthorizedAccess();
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
        .orElseThrow(() -> new ApplicationException.NotFound(applicationId));

    if (!application.getEmployee().getUserId().equals(userId)) {
      throw new ApplicationSecurityException.UnauthorizedCancellation();
    }
    if (application.getStatus() != ApplicationStatus.PENDING) {
      throw new ApplicationException.NotPending();
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
