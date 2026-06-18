package pl.chrono.applicationmanager.service;

import jakarta.persistence.EntityManager;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import pl.chrono.applicationmanager.TestConfig;
import pl.chrono.applicationmanager.dto.request.CreateApplicationRequest;
import pl.chrono.applicationmanager.dto.response.ApplicationResponse;
import pl.chrono.applicationmanager.exception.ApplicationException;
import pl.chrono.applicationmanager.exception.ApplicationSecurityException;
import pl.chrono.applicationmanager.exception.ApplicationUserException;
import pl.chrono.dbmanager.model.Application;
import pl.chrono.dbmanager.model.Role;
import pl.chrono.dbmanager.model.User;
import pl.chrono.dbmanager.model.enums.ApplicationStatus;
import pl.chrono.dbmanager.model.enums.ApplicationType;
import pl.chrono.dbmanager.repository.ApplicationRepository;
import pl.chrono.dbmanager.repository.RoleRepository;
import pl.chrono.dbmanager.repository.UserRepository;
import pl.chrono.timemanager.service.OvertimeService;

@SpringBootTest(classes = TestConfig.class)
@Transactional
class ApplicationServiceIT {

  @Autowired
  private ApplicationService applicationService;

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private ApplicationRepository applicationRepository;

  @Autowired
  private RoleRepository roleRepository;

  @Autowired
  private EntityManager entityManager;

  @MockitoBean
  private OvertimeService overtimeService;

  private User employee;
  private User manager;
  private User hrUser;
  private User adminUser;
  private User otherManager;

  @BeforeEach
  void setUp() {
    manager = persistUser("manager@chrono.pl", "ROLE_MANAGER", null);
    otherManager = persistUser("other.manager@chrono.pl", "ROLE_MANAGER", null);
    employee = persistUser("pracownik@chrono.pl", "ROLE_EMPLOYEE", manager);
    hrUser = persistUser("hr@chrono.pl", "ROLE_HR", null);
    adminUser = persistUser("admin@chrono.pl", "ROLE_ADMIN", null);

    entityManager.flush();
    entityManager.clear();
  }

  @Test
  void cancelApplication_shouldSetStatusToCancelledInDatabase() {
    Application app = persistApplication(employee, ApplicationType.SICK_LEAVE, ApplicationStatus.PENDING);

    ApplicationResponse response = applicationService.cancelApplication(app.getId(), employee.getUserId());

    assertThat(response.status()).isEqualTo("CANCELLED");

    entityManager.flush();
    entityManager.clear();

    Application cancelledApp = applicationRepository.findById(app.getId()).orElseThrow();
    assertThat(cancelledApp.getStatus()).isEqualTo(ApplicationStatus.CANCELLED);
    assertThat(cancelledApp.getDecisionDateTime()).isNotNull();
    verify(overtimeService, never()).refundOvertimeForApplication(any());
  }

  @Test
  void cancelApplication_shouldRefundOvertime_whenTypeIsOvertimeTake() {
    Application app = persistApplication(employee, ApplicationType.OVERTIME_TAKE, ApplicationStatus.PENDING);

    applicationService.cancelApplication(app.getId(), employee.getUserId());

    verify(overtimeService, times(1)).refundOvertimeForApplication(any(Application.class));
  }

  @Test
  void cancelApplication_shouldThrowException_whenUserIsNotOwner() {
    Application app = persistApplication(employee, ApplicationType.SICK_LEAVE, ApplicationStatus.PENDING);

    assertThatThrownBy(() ->
        applicationService.cancelApplication(app.getId(), manager.getUserId())
    ).isInstanceOf(ApplicationSecurityException.UnauthorizedCancellation.class);
  }

  @Test
  void cancelApplication_shouldThrowException_whenApplicationIsNotPending() {
    Application app = persistApplication(employee, ApplicationType.HOLIDAY_LEAVE, ApplicationStatus.APPROVED);

    assertThatThrownBy(() ->
        applicationService.cancelApplication(app.getId(), employee.getUserId())
    ).isInstanceOf(ApplicationException.NotPending.class);
  }

  @Test
  void changeStatus_shouldUpdateStatusAndReviewerInDatabase() {
    Application app = persistApplication(employee, ApplicationType.HOLIDAY_LEAVE, ApplicationStatus.PENDING);

    ApplicationResponse response = applicationService.changeStatus(
        app.getId(), "APPROVED", manager.getUserId(), "Zgoda na urlop", false
    );

    assertThat(response.status()).isEqualTo("APPROVED");

    entityManager.flush();
    entityManager.clear();

    Application updatedApp = applicationRepository.findById(app.getId()).orElseThrow();
    assertThat(updatedApp.getStatus()).isEqualTo(ApplicationStatus.APPROVED);
    assertThat(updatedApp.getDecisionReason()).isEqualTo("Zgoda na urlop");
    assertThat(updatedApp.getReviewer().getUserId()).isEqualTo(manager.getUserId());
    assertThat(updatedApp.getDecisionDateTime()).isNotNull();
  }

  @Test
  void changeStatus_shouldRefundOvertime_whenTypeIsOvertimeTakeAndRejected() {
    Application app = persistApplication(employee, ApplicationType.OVERTIME_TAKE, ApplicationStatus.PENDING);

    applicationService.changeStatus(app.getId(), "REJECTED", manager.getUserId(), "Brak zgody", false);

    verify(overtimeService, times(1)).refundOvertimeForApplication(any(Application.class));
  }

  @Test
  void changeStatus_shouldNotRefundOvertime_whenTypeIsOvertimeTakeAndApproved() {
    Application app = persistApplication(employee, ApplicationType.OVERTIME_TAKE, ApplicationStatus.PENDING);

    applicationService.changeStatus(app.getId(), "APPROVED", manager.getUserId(), "Zgoda", false);

    verify(overtimeService, never()).refundOvertimeForApplication(any(Application.class));
  }

  @Test
  void changeStatus_shouldAllowAdminToOverrideStatus() {
    Application app = persistApplication(employee, ApplicationType.HOLIDAY_LEAVE, ApplicationStatus.PENDING);

    ApplicationResponse response = applicationService.changeStatus(
        app.getId(), "REJECTED", adminUser.getUserId(), "Decyzja odgórna", true
    );

    assertThat(response.status()).isEqualTo("REJECTED");
    assertThat(response.reviewerId()).isEqualTo(adminUser.getUserId());
  }

  @Test
  void changeStatus_shouldThrowException_whenManagerIsUnauthorized() {
    Application app = persistApplication(employee, ApplicationType.HOLIDAY_LEAVE, ApplicationStatus.PENDING);

    assertThatThrownBy(() ->
        applicationService.changeStatus(app.getId(), "REJECTED", otherManager.getUserId(), "Odmowa", false)
    ).isInstanceOf(ApplicationSecurityException.UnauthorizedManager.class);
  }

  @Test
  void changeStatus_shouldThrowException_whenApplicationAlreadyProcessed() {
    Application app = persistApplication(employee, ApplicationType.HOLIDAY_LEAVE, ApplicationStatus.REJECTED);

    assertThatThrownBy(() ->
        applicationService.changeStatus(app.getId(), "APPROVED", manager.getUserId(), "Zmiana zdania", false)
    ).isInstanceOf(ApplicationException.AlreadyProcessed.class);
  }

  @Test
  void changeStatus_shouldThrowException_whenApplicationNotFound() {
    assertThatThrownBy(() ->
        applicationService.changeStatus(999L, "APPROVED", manager.getUserId(), "Zgoda", false)
    ).isInstanceOf(ApplicationException.NotFound.class);
  }

  @Test
  void createApplicationForEmployee_byHR_shouldCreateAndAutoApprove() {
    CreateApplicationRequest request = new CreateApplicationRequest(
        "HOME_OFFICE", LocalDate.now(), LocalDate.now(), "Praca zdalna", 0
    );

    ApplicationResponse response = applicationService.createApplicationForEmployee(
        request, employee.getUserId(), hrUser.getUserId()
    );

    assertThat(response.status()).isEqualTo("APPROVED");

    Application savedApp = applicationRepository.findById(response.id()).orElseThrow();
    assertThat(savedApp.getStatus()).isEqualTo(ApplicationStatus.APPROVED);
    assertThat(savedApp.getReviewer().getUserId()).isEqualTo(hrUser.getUserId());
  }

  @Test
  void createApplicationForEmployee_byDirectManager_shouldCreateAndAutoApprove() {
    CreateApplicationRequest request = new CreateApplicationRequest(
        "SICK_LEAVE", LocalDate.now(), LocalDate.now(), "Chorobowe L4", 0
    );

    ApplicationResponse response = applicationService.createApplicationForEmployee(
        request, employee.getUserId(), manager.getUserId()
    );

    assertThat(response.status()).isEqualTo("APPROVED");
  }

  @Test
  void createApplicationForEmployee_byAdmin_shouldCreateAndAutoApprove() {
    CreateApplicationRequest request = new CreateApplicationRequest(
        "HOLIDAY_LEAVE", LocalDate.now(), LocalDate.now(), "Urlop awaryjny", 0
    );

    ApplicationResponse response = applicationService.createApplicationForEmployee(
        request, employee.getUserId(), adminUser.getUserId()
    );

    assertThat(response.status()).isEqualTo("APPROVED");
  }

  @Test
  void createApplicationForEmployee_byOtherManager_shouldThrowUnauthorizedException() {
    CreateApplicationRequest request = new CreateApplicationRequest(
        "HOLIDAY_LEAVE", LocalDate.now(), LocalDate.now(), "Urlop", 0
    );

    assertThatThrownBy(() ->
        applicationService.createApplicationForEmployee(request, employee.getUserId(), otherManager.getUserId())
    ).isInstanceOf(ApplicationSecurityException.UnauthorizedCreation.class);
  }

  @Test
  void createApplicationForEmployee_shouldThrowException_whenEmployeeNotFound() {
    CreateApplicationRequest request = new CreateApplicationRequest(
        "HOLIDAY_LEAVE", LocalDate.now(), LocalDate.now(), "Urlop", 0
    );

    assertThatThrownBy(() ->
        applicationService.createApplicationForEmployee(request, 999, manager.getUserId())
    ).isInstanceOf(ApplicationUserException.NotFound.class);
  }

  @Test
  void createApplication_shouldPersistNewApplicationInDatabase() {
    CreateApplicationRequest request = new CreateApplicationRequest(
        "SICK_LEAVE", LocalDate.now(), LocalDate.now().plusDays(2), "Grypa", 0
    );

    ApplicationResponse response = applicationService.createApplication(request, employee.getUserId());

    assertThat(response).isNotNull();
    assertThat(response.status()).isEqualTo("PENDING");

    Application savedApp = applicationRepository.findById(response.id()).orElseThrow();
    assertThat(savedApp.getType()).isEqualTo(ApplicationType.SICK_LEAVE);
    assertThat(savedApp.getReason()).isEqualTo("Grypa");
    assertThat(savedApp.getEmployee().getUserId()).isEqualTo(employee.getUserId());
  }

  @Test
  void createApplication_shouldReserveOvertime_whenTypeIsOvertimeTake() {
    CreateApplicationRequest request = new CreateApplicationRequest(
        "OVERTIME_TAKE", LocalDate.now(), LocalDate.now(), "Odbiór godzin", 3600
    );

    applicationService.createApplication(request, employee.getUserId());

    verify(overtimeService, times(1)).reserveOvertimeForApplication(any(Application.class));
  }

  @Test
  void createApplication_shouldThrowException_whenUserNotFound() {
    CreateApplicationRequest request = new CreateApplicationRequest(
        "SICK_LEAVE", LocalDate.now(), LocalDate.now(), "Test", 0
    );

    assertThatThrownBy(() ->
        applicationService.createApplication(request, 999)
    ).isInstanceOf(ApplicationUserException.NotFound.class);
  }

  @Test
  void getPendingApplicationsForManager_returnsOnlyPendingFromSubordinates() {
    persistApplication(employee, ApplicationType.HOLIDAY_LEAVE, ApplicationStatus.PENDING);
    persistApplication(employee, ApplicationType.SICK_LEAVE, ApplicationStatus.APPROVED);

    User otherEmployee = persistUser("inny@chrono.pl", "ROLE_EMPLOYEE", otherManager);
    persistApplication(otherEmployee, ApplicationType.HOME_OFFICE, ApplicationStatus.PENDING);

    entityManager.flush();
    entityManager.clear();

    List<ApplicationResponse> pendingForManager = applicationService.getPendingApplicationsForManager(manager.getUserId());

    assertThat(pendingForManager).hasSize(1);
    assertThat(pendingForManager.getFirst().type()).isEqualTo("HOLIDAY_LEAVE");
    assertThat(pendingForManager.getFirst().employeeId()).isEqualTo(employee.getUserId());
  }

  @Test
  void getAllApplicationsForManager_returnsAllFromSubordinates() {
    persistApplication(employee, ApplicationType.HOLIDAY_LEAVE, ApplicationStatus.PENDING);
    persistApplication(employee, ApplicationType.SICK_LEAVE, ApplicationStatus.APPROVED);

    entityManager.flush();
    entityManager.clear();

    List<ApplicationResponse> allForManager = applicationService.getAllApplicationsForManager(manager.getUserId());

    assertThat(allForManager).hasSize(2);
    assertThat(allForManager).extracting(ApplicationResponse::status)
        .containsExactlyInAnyOrder("PENDING", "APPROVED");
  }

  @Test
  void getUserApplications_returnsApplications_whenRequestedByDirectManager() {
    persistApplication(employee, ApplicationType.HOLIDAY_LEAVE, ApplicationStatus.PENDING);

    entityManager.flush();
    entityManager.clear();

    List<ApplicationResponse> apps = applicationService.getUserApplications(employee.getUserId(), manager.getUserId());

    assertThat(apps).hasSize(1);
  }

  @Test
  void getUserApplications_returnsApplications_whenRequestedByHR() {
    persistApplication(employee, ApplicationType.HOLIDAY_LEAVE, ApplicationStatus.PENDING);

    entityManager.flush();
    entityManager.clear();

    List<ApplicationResponse> apps = applicationService.getUserApplications(employee.getUserId(), hrUser.getUserId());

    assertThat(apps).hasSize(1);
  }

  @Test
  void getUserApplications_shouldThrowUnauthorized_whenRequesterHasNoAccess() {
    persistApplication(employee, ApplicationType.HOLIDAY_LEAVE, ApplicationStatus.PENDING);

    assertThatThrownBy(() ->
        applicationService.getUserApplications(employee.getUserId(), otherManager.getUserId())
    ).isInstanceOf(ApplicationSecurityException.UnauthorizedAccess.class);
  }

  @Test
  void getAllApplications_returnsAll_whenRequestedByAdmin() {
    persistApplication(employee, ApplicationType.HOLIDAY_LEAVE, ApplicationStatus.PENDING);

    User otherEmployee = persistUser("inny@chrono.pl", "ROLE_EMPLOYEE", otherManager);
    persistApplication(otherEmployee, ApplicationType.HOME_OFFICE, ApplicationStatus.APPROVED);

    entityManager.flush();
    entityManager.clear();

    List<ApplicationResponse> allApps = applicationService.getAllApplications();

    assertThat(allApps).hasSize(2);
  }

  private User persistUser(String email, String roleName, User assignedManager) {
    Role role = roleRepository.findByName(roleName)
        .orElseGet(() -> {
          Role r = new Role();
          r.setName(roleName);
          return roleRepository.save(r);
        });

    User user = new User();
    user.setEmail(email);
    user.setName("Test");
    user.setSurname("User");
    user.setPasswordHash("hash");
    user.setRoles(Set.of(role));
    user.setManager(assignedManager);

    return userRepository.save(user);
  }

  private Application persistApplication(User user, ApplicationType type, ApplicationStatus status) {
    Application app = new Application();
    app.setEmployee(user);
    app.setType(type);
    app.setStatus(status);
    app.setStartDate(LocalDate.now());
    app.setEndDate(LocalDate.now().plusDays(1));
    app.setCreationDateTime(java.time.LocalDateTime.now());
    app.setDurationSeconds(0);

    Application savedApp = applicationRepository.save(app);
    entityManager.flush();
    entityManager.clear();

    return savedApp;
  }
}
