package pl.chrono.applicationmanager;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import pl.chrono.applicationmanager.event.ApplicationNotificationEvent;
import pl.chrono.applicationmanager.dto.request.CreateApplicationRequest;
import pl.chrono.applicationmanager.dto.response.ApplicationResponse;
import pl.chrono.applicationmanager.service.ApplicationService;
import pl.chrono.applicationmanager.exception.ApplicationException;
import pl.chrono.applicationmanager.exception.ApplicationSecurityException;
import pl.chrono.applicationmanager.exception.ApplicationUserException;
import pl.chrono.dbmanager.model.Application;
import pl.chrono.dbmanager.model.User;
import pl.chrono.dbmanager.model.Role;
import pl.chrono.dbmanager.model.enums.ApplicationStatus;
import pl.chrono.dbmanager.model.enums.ApplicationType;
import pl.chrono.dbmanager.repository.ApplicationRepository;
import pl.chrono.dbmanager.repository.UserRepository;
import pl.chrono.timemanager.service.ApplicationValidationService;

@ExtendWith(MockitoExtension.class)
class ApplicationManagerServiceTest {

  @Mock
  private ApplicationRepository applicationRepository;

  @Mock
  private UserRepository userRepository;

  @Mock
  private ApplicationEventPublisher eventPublisher;

  @Mock
  private ApplicationValidationService applicationValidationService;

  @InjectMocks
  private ApplicationService applicationService;

  @Test
  void createApplication_ShouldReturnDto_WhenUserExists() {
    Integer employeeId = 1;

    User mockUser = new User();
    mockUser.setUserId(employeeId);
    mockUser.setEmail("pracownik@chrono.pl");

    CreateApplicationRequest requestDto = new CreateApplicationRequest(
        "OVERTIME_EARN",
        LocalDate.of(2026, 5, 1),
        LocalDate.of(2026, 5, 1),
        "Projekt nadgodziny",
        7200
    );

    Application savedApplication = new Application();
    savedApplication.setId(100L);
    savedApplication.setEmployee(mockUser);
    savedApplication.setType(ApplicationType.OVERTIME_EARN);
    savedApplication.setStatus(ApplicationStatus.PENDING);
    savedApplication.setStartDate(requestDto.startDate());
    savedApplication.setEndDate(requestDto.endDate());
    savedApplication.setReason(requestDto.reason());
    savedApplication.setDurationSeconds(7200);
    savedApplication.setCreationDateTime(java.time.LocalDateTime.now());

    when(userRepository.findById(employeeId)).thenReturn(Optional.of(mockUser));
    when(applicationRepository.save(any(Application.class))).thenReturn(savedApplication);

    ApplicationResponse result = applicationService.createApplication(requestDto, employeeId);

    assertNotNull(result);
    assertEquals(100L, result.id());
    assertEquals("OVERTIME_EARN", result.type());
    assertEquals("PENDING", result.status());
    assertEquals(employeeId, result.employeeId());
    assertEquals(7200, result.durationSeconds());
    assertNotNull(result.creationDateTime());

    verify(applicationRepository, times(1)).save(any(Application.class));
    verify(eventPublisher, times(1)).publishEvent(any(ApplicationNotificationEvent.class));
  }

  @Test
  void changeStatus_ShouldUpdateStatusAndReviewer_WhenBothExist() {
    Long applicationId = 1L;
    Integer reviewerId = 2;
    String newStatus = "APPROVED";
    String decisionReason = "Valid reason";

    User mockReviewer = new User();
    mockReviewer.setUserId(reviewerId);
    mockReviewer.setEmail("manager@chrono.pl");
    mockReviewer.setName("Jan");
    mockReviewer.setSurname("Kowalski");
    User mockEmployee = new User();
    mockEmployee.setUserId(1);
    mockEmployee.setEmail("pracownik@chrono.pl");
    mockEmployee.setManager(mockReviewer);

    Application mockApplication = new Application();
    mockApplication.setId(applicationId);
    mockApplication.setStatus(ApplicationStatus.PENDING);
    mockApplication.setEmployee(mockEmployee);
    mockApplication.setType(ApplicationType.HOLIDAY_LEAVE);
    mockApplication.setDurationSeconds(0);

    when(applicationRepository.findById(applicationId)).thenReturn(Optional.of(mockApplication));
    when(userRepository.findById(reviewerId)).thenReturn(Optional.of(mockReviewer));
    when(applicationRepository.save(any(Application.class))).thenReturn(mockApplication);

    ApplicationResponse result = applicationService.changeStatus(applicationId, newStatus, reviewerId, decisionReason, false);

    assertNotNull(result);
    assertEquals(newStatus, result.status());
    assertEquals(decisionReason, result.decisionReason());
    assertEquals(reviewerId, result.reviewerId());
    assertNotNull(result.decisionDateTime());

    verify(applicationRepository, times(1)).save(mockApplication);
    verify(eventPublisher, times(1)).publishEvent(any(ApplicationNotificationEvent.class));
  }

  @Test
  void getUserApplications_ShouldReturnListOfDtos() {
    Integer employeeId = 1;
    Integer requesterId = 1;

    User mockEmployee = new User();
    mockEmployee.setUserId(employeeId);

    Application mockApplication = new Application();
    mockApplication.setId(10L);
    mockApplication.setEmployee(mockEmployee);
    mockApplication.setType(ApplicationType.SICK_LEAVE);
    mockApplication.setStatus(ApplicationStatus.PENDING);

    when(applicationRepository.findAllByEmployeeUserId(employeeId))
        .thenReturn(List.of(mockApplication));

    List<ApplicationResponse> result = applicationService.getUserApplications(employeeId, requesterId);

    assertNotNull(result);
    assertEquals(1, result.size());
    assertEquals(10L, result.getFirst().id());
    assertEquals("SICK_LEAVE", result.getFirst().type());
  }

  @Test
  void getPendingApplicationsForManager_ShouldReturnListOfDtos() {
    Integer managerId = 2;

    User mockEmployee = new User();
    mockEmployee.setUserId(1);

    Application mockApplication = new Application();
    mockApplication.setId(20L);
    mockApplication.setEmployee(mockEmployee);
    mockApplication.setType(ApplicationType.EARLY_EXIT);
    mockApplication.setStatus(ApplicationStatus.PENDING);

    when(applicationRepository.findPendingByManagerId(ApplicationStatus.PENDING, managerId))
        .thenReturn(List.of(mockApplication));

    List<ApplicationResponse> result = applicationService.getPendingApplicationsForManager(managerId);

    assertNotNull(result);
    assertEquals(1, result.size());
    assertEquals(20L, result.getFirst().id());
    assertEquals("PENDING", result.getFirst().status());

    verify(applicationRepository, times(1)).findPendingByManagerId(ApplicationStatus.PENDING, managerId);
  }

  @Test
  void createApplication_ShouldThrowException_WhenUserDoesNotExist() {
    Integer employeeId = 999;
    CreateApplicationRequest requestDto = new CreateApplicationRequest(
        "EARLY_EXIT", LocalDate.now(), LocalDate.now(), "Test", 3600
    );

    when(userRepository.findById(employeeId)).thenReturn(Optional.empty());

    RuntimeException exception = assertThrows(RuntimeException.class, () ->
        applicationService.createApplication(requestDto, employeeId)
    );
    assertThrows(ApplicationUserException.NotFound.class, () ->
        applicationService.createApplication(requestDto, employeeId)
    );
  }

  @Test
  void changeStatus_ShouldThrowException_WhenApplicationNotPending() {
    Long applicationId = 1L;
    Application mockApplication = new Application();
    mockApplication.setId(applicationId);
    mockApplication.setStatus(ApplicationStatus.APPROVED);

    when(applicationRepository.findById(applicationId)).thenReturn(Optional.of(mockApplication));

    assertThrows(ApplicationException.AlreadyProcessed.class, () ->
        applicationService.changeStatus(applicationId, "REJECTED", 2, "Reason", false)
    );
  }

  @Test
  void changeStatus_ShouldThrowException_WhenEmployeeHasNoManager() {
    Long applicationId = 1L;
    Application mockApplication = new Application();
    mockApplication.setId(applicationId);
    mockApplication.setStatus(ApplicationStatus.PENDING);

    User mockEmployee = new User();
    mockEmployee.setUserId(1);
    mockEmployee.setManager(null);
    mockApplication.setEmployee(mockEmployee);

    when(applicationRepository.findById(applicationId)).thenReturn(Optional.of(mockApplication));

    assertThrows(ApplicationSecurityException.UnauthorizedManager.class, () ->
        applicationService.changeStatus(applicationId, "APPROVED", 2, "Reason", false)
    );
  }

  @Test
  void changeStatus_ShouldThrowException_WhenReviewerIsNotDirectManager() {
    Long applicationId = 1L;
    Integer actualManagerId = 3;
    Integer fakeReviewerId = 2;

    User actualManager = new User();
    actualManager.setUserId(actualManagerId);

    User mockEmployee = new User();
    mockEmployee.setUserId(1);
    mockEmployee.setManager(actualManager);

    Application mockApplication = new Application();
    mockApplication.setId(applicationId);
    mockApplication.setStatus(ApplicationStatus.PENDING);
    mockApplication.setEmployee(mockEmployee);

    when(applicationRepository.findById(applicationId)).thenReturn(Optional.of(mockApplication));

    assertThrows(ApplicationSecurityException.UnauthorizedManager.class, () ->
        applicationService.changeStatus(applicationId, "APPROVED", fakeReviewerId, "Reason", false)
    );
  }

  @Test
  void getUserApplications_ShouldThrowException_WhenRequesterHasNoAccess() {
    Integer employeeId = 1;
    Integer requesterId = 2;

    User mockEmployee = new User();
    mockEmployee.setUserId(employeeId);
    mockEmployee.setManager(null);

    User mockRequester = new User();
    mockRequester.setUserId(requesterId);
    mockRequester.setRoles(Set.of());

    when(userRepository.findById(employeeId)).thenReturn(Optional.of(mockEmployee));
    when(userRepository.findById(requesterId)).thenReturn(Optional.of(mockRequester));

    assertThrows(ApplicationSecurityException.UnauthorizedAccess.class, () ->
        applicationService.getUserApplications(employeeId, requesterId)
    );
  }

  @Test
  void getUserApplications_ShouldReturnDtos_WhenRequesterIsHR() {
    Integer employeeId = 1;
    Integer requesterId = 2;

    User mockEmployee = new User();
    mockEmployee.setUserId(employeeId);

    Role hrRole = new Role();
    hrRole.setName("ROLE_HR");

    User mockRequester = new User();
    mockRequester.setUserId(requesterId);
    mockRequester.setRoles(Set.of(hrRole));

    Application mockApplication = new Application();
    mockApplication.setId(10L);
    mockApplication.setEmployee(mockEmployee);
    mockApplication.setType(ApplicationType.SICK_LEAVE);
    mockApplication.setStatus(ApplicationStatus.PENDING);

    when(userRepository.findById(employeeId)).thenReturn(Optional.of(mockEmployee));
    when(userRepository.findById(requesterId)).thenReturn(Optional.of(mockRequester));
    when(applicationRepository.findAllByEmployeeUserId(employeeId)).thenReturn(List.of(mockApplication));

    List<ApplicationResponse> result = applicationService.getUserApplications(employeeId, requesterId);

    assertNotNull(result);
    assertEquals(1, result.size());
  }

  @Test
  void getAllApplicationsForManager_ShouldReturnListOfDtos() {
    Integer managerId = 2;

    User mockEmployee = new User();
    mockEmployee.setUserId(1);

    Application mockApplication = new Application();
    mockApplication.setId(30L);
    mockApplication.setEmployee(mockEmployee);
    mockApplication.setType(ApplicationType.OVERTIME_TAKE);
    mockApplication.setStatus(ApplicationStatus.APPROVED);

    when(applicationRepository.findAllByManagerId(managerId))
        .thenReturn(List.of(mockApplication));

    List<ApplicationResponse> result = applicationService.getAllApplicationsForManager(managerId);

    assertNotNull(result);
    assertEquals(1, result.size());
    assertEquals(30L, result.getFirst().id());

    verify(applicationRepository, times(1)).findAllByManagerId(managerId);
  }

  @Test
  void createApplication_ShouldReturnDto_WhenEmployeeHasManager() {
    Integer employeeId = 1;

    User mockManager = new User();
    mockManager.setUserId(2);

    User mockUser = new User();
    mockUser.setUserId(employeeId);
    mockUser.setEmail("pracownik@chrono.pl");
    mockUser.setManager(mockManager);

    CreateApplicationRequest requestDto = new CreateApplicationRequest(
        "OVERTIME_EARN",
        LocalDate.of(2026, 5, 1),
        LocalDate.of(2026, 5, 1),
        null,
        null
    );

    Application savedApplication = new Application();
    savedApplication.setId(100L);
    savedApplication.setEmployee(mockUser);
    savedApplication.setType(ApplicationType.OVERTIME_EARN);
    savedApplication.setStatus(ApplicationStatus.PENDING);
    savedApplication.setStartDate(requestDto.startDate());
    savedApplication.setEndDate(requestDto.endDate());
    savedApplication.setCreationDateTime(java.time.LocalDateTime.now());

    when(userRepository.findById(employeeId)).thenReturn(Optional.of(mockUser));
    when(applicationRepository.save(any(Application.class))).thenReturn(savedApplication);

    ApplicationResponse result = applicationService.createApplication(requestDto, employeeId);

    assertNotNull(result);
    verify(eventPublisher, times(1)).publishEvent(any(pl.chrono.applicationmanager.event.ApplicationNotificationEvent.class));
  }

  @Test
  void changeStatus_ShouldThrowException_WhenReviewerNotFound() {
    Long applicationId = 1L;
    Integer reviewerId = 2;

    User mockManager = new User();
    mockManager.setUserId(reviewerId);

    User mockEmployee = new User();
    mockEmployee.setUserId(1);
    mockEmployee.setManager(mockManager);

    Application mockApplication = new Application();
    mockApplication.setId(applicationId);
    mockApplication.setStatus(ApplicationStatus.PENDING);
    mockApplication.setEmployee(mockEmployee);

    when(applicationRepository.findById(applicationId)).thenReturn(Optional.of(mockApplication));
    when(userRepository.findById(reviewerId)).thenReturn(Optional.empty());

    assertThrows(ApplicationUserException.NotFound.class, () ->
        applicationService.changeStatus(applicationId, "APPROVED", reviewerId, "Reason", false)
    );
  }

  @Test
  void getUserApplications_ShouldThrowException_WhenEmployeeNotFound() {
    when(userRepository.findById(1)).thenReturn(Optional.empty());
    assertThrows(ApplicationUserException.NotFound.class, () -> applicationService.getUserApplications(1, 2));
  }

  @Test
  void getUserApplications_ShouldThrowException_WhenRequesterNotFound() {
    User employee = new User();
    when(userRepository.findById(1)).thenReturn(Optional.of(employee));
    when(userRepository.findById(2)).thenReturn(Optional.empty());

    assertThrows(ApplicationUserException.NotFound.class, () -> applicationService.getUserApplications(1, 2));
  }

  @Test
  void getUserApplications_ShouldReturnDtos_WhenRequesterIsDirectManager() {
    Integer employeeId = 1;
    Integer requesterId = 2;

    User mockRequester = new User();
    mockRequester.setUserId(requesterId);
    mockRequester.setRoles(Set.of());

    User mockEmployee = new User();
    mockEmployee.setUserId(employeeId);
    mockEmployee.setManager(mockRequester);

    when(userRepository.findById(employeeId)).thenReturn(Optional.of(mockEmployee));
    when(userRepository.findById(requesterId)).thenReturn(Optional.of(mockRequester));
    when(applicationRepository.findAllByEmployeeUserId(employeeId)).thenReturn(List.of());

    List<ApplicationResponse> result = applicationService.getUserApplications(employeeId, requesterId);
    assertNotNull(result);
  }

  @Test
  void getUserApplications_ShouldReturnDtos_WhenRequesterIsAdmin() {
    Integer employeeId = 1;
    Integer requesterId = 2;

    User mockEmployee = new User();
    mockEmployee.setUserId(employeeId);

    Role adminRole = new Role();
    adminRole.setName("ROLE_ADMIN");

    User mockRequester = new User();
    mockRequester.setUserId(requesterId);
    mockRequester.setRoles(Set.of(adminRole));

    when(userRepository.findById(employeeId)).thenReturn(Optional.of(mockEmployee));
    when(userRepository.findById(requesterId)).thenReturn(Optional.of(mockRequester));
    when(applicationRepository.findAllByEmployeeUserId(employeeId)).thenReturn(List.of());

    List<ApplicationResponse> result = applicationService.getUserApplications(employeeId, requesterId);
    assertNotNull(result);
  }

  @Test
  void getAllApllications_ShouldReturnAllAlplication_WhenRequesterIsAdmin() {
    User mockEmployee = new User();
    mockEmployee.setUserId(1);
    mockEmployee.setEmail("pracownik@chrono.pl");

    Application mockApp1 = new Application();
    mockApp1.setId(10L);
    mockApp1.setEmployee(mockEmployee);
    mockApp1.setType(ApplicationType.HOLIDAY_LEAVE);
    mockApp1.setStatus(ApplicationStatus.PENDING);

    Application mockApp2 = new Application();
    mockApp2.setId(11L);
    mockApp2.setEmployee(mockEmployee);
    mockApp2.setType(ApplicationType.SICK_LEAVE);
    mockApp2.setStatus(ApplicationStatus.APPROVED);

    when(applicationRepository.findAll()).thenReturn(List.of(mockApp1, mockApp2));

    List<ApplicationResponse> result = applicationService.getAllApplications();

    assertNotNull(result);
    assertEquals(2, result.size());
    assertEquals(10L, result.get(0).id());
    assertEquals("HOLIDAY_LEAVE", result.get(0).type());
    assertEquals("PENDING", result.get(0).status());
    assertEquals(11L, result.get(1).id());
    assertEquals("SICK_LEAVE", result.get(1).type());
    assertEquals("APPROVED", result.get(1).status());

    verify(applicationRepository, times(1)).findAll();
  }

  @Test
  void changeStatus_ShouldUpdateStatus_WhenUserIsAdminAndNotManager() {
    Long applicationId = 1L;
    Integer adminId = 99;
    String newStatus = "APPROVED";
    String decisionReason = "Admin override";

    User mockAdmin = new User();
    mockAdmin.setUserId(adminId);
    mockAdmin.setName("Admin");
    mockAdmin.setSurname("Testowy");

    User mockEmployee = new User();
    mockEmployee.setUserId(1);
    mockEmployee.setManager(null);

    Application mockApplication = new Application();
    mockApplication.setId(applicationId);
    mockApplication.setStatus(ApplicationStatus.PENDING);
    mockApplication.setEmployee(mockEmployee);
    mockApplication.setType(ApplicationType.HOLIDAY_LEAVE);

    when(applicationRepository.findById(applicationId)).thenReturn(Optional.of(mockApplication));
    when(userRepository.findById(adminId)).thenReturn(Optional.of(mockAdmin));
    when(applicationRepository.save(any(Application.class))).thenReturn(mockApplication);

    ApplicationResponse result = applicationService.changeStatus(applicationId, newStatus, adminId, decisionReason, true);

    assertNotNull(result);
    assertEquals(newStatus, result.status());
    assertEquals(adminId, result.reviewerId());

    verify(applicationRepository, times(1)).save(mockApplication);
    verify(eventPublisher, times(1)).publishEvent(any(ApplicationNotificationEvent.class));
  }
}
