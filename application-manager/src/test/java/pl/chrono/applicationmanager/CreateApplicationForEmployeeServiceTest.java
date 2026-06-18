package pl.chrono.applicationmanager;

import java.time.LocalDate;
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

import pl.chrono.applicationmanager.dto.request.CreateApplicationRequest;
import pl.chrono.applicationmanager.dto.response.ApplicationResponse;
import pl.chrono.applicationmanager.event.ApplicationNotificationEvent;
import pl.chrono.applicationmanager.exception.ApplicationSecurityException;
import pl.chrono.applicationmanager.exception.ApplicationUserException;
import pl.chrono.applicationmanager.service.ApplicationService;
import pl.chrono.dbmanager.model.Application;
import pl.chrono.dbmanager.model.Role;
import pl.chrono.dbmanager.model.User;
import pl.chrono.dbmanager.model.enums.ApplicationStatus;
import pl.chrono.dbmanager.model.enums.ApplicationType;
import pl.chrono.dbmanager.repository.ApplicationRepository;
import pl.chrono.dbmanager.repository.UserRepository;
import pl.chrono.timemanager.service.ApplicationValidationService;

@ExtendWith(MockitoExtension.class)
class CreateApplicationForEmployeeServiceTest {

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

  private CreateApplicationRequest createRequest() {
    return new CreateApplicationRequest("HOLIDAY_LEAVE", LocalDate.now(), LocalDate.now(), "Reason", 28800);
  }

  @Test
  void createApplicationForEmployee_ShouldThrowNotFound_WhenManagerDoesNotExist() {
    when(userRepository.findById(1)).thenReturn(Optional.empty());

    assertThrows(ApplicationUserException.NotFound.class, () ->
        applicationService.createApplicationForEmployee(createRequest(), 2, 1)
    );
  }

  @Test
  void createApplicationForEmployee_ShouldThrowNotFound_WhenEmployeeDoesNotExist() {
    User manager = new User();
    manager.setUserId(1);

    when(userRepository.findById(1)).thenReturn(Optional.of(manager));
    when(userRepository.findById(2)).thenReturn(Optional.empty());

    assertThrows(ApplicationUserException.NotFound.class, () ->
        applicationService.createApplicationForEmployee(createRequest(), 2, 1)
    );
  }

  @Test
  void createApplicationForEmployee_ShouldThrowUnauthorized_WhenNoAccess() {
    User manager = new User();
    manager.setUserId(1);
    manager.setRoles(Set.of());

    User employee = new User();
    employee.setUserId(2);
    employee.setManager(new User());
    employee.getManager().setUserId(3);

    when(userRepository.findById(1)).thenReturn(Optional.of(manager));
    when(userRepository.findById(2)).thenReturn(Optional.of(employee));

    assertThrows(ApplicationSecurityException.UnauthorizedCreation.class, () ->
        applicationService.createApplicationForEmployee(createRequest(), 2, 1)
    );
  }

  @Test
  void createApplicationForEmployee_ShouldCreateAndApprove_WhenUserIsDirectManager() {
    Integer managerId = 1;
    Integer employeeId = 2;

    User manager = new User();
    manager.setUserId(managerId);
    manager.setRoles(Set.of());

    User employee = new User();
    employee.setUserId(employeeId);
    employee.setManager(manager);

    Application savedApp = new Application();
    savedApp.setId(10L);
    savedApp.setEmployee(employee);
    savedApp.setType(ApplicationType.HOLIDAY_LEAVE);
    savedApp.setStatus(ApplicationStatus.PENDING);

    Application approvedApp = new Application();
    approvedApp.setId(10L);
    approvedApp.setEmployee(employee);
    approvedApp.setType(ApplicationType.HOLIDAY_LEAVE);
    approvedApp.setStatus(ApplicationStatus.APPROVED);

    when(userRepository.findById(managerId)).thenReturn(Optional.of(manager));
    when(userRepository.findById(employeeId)).thenReturn(Optional.of(employee));
    when(applicationRepository.save(any(Application.class))).thenReturn(savedApp).thenReturn(approvedApp);
    when(applicationRepository.findById(10L)).thenReturn(Optional.of(savedApp));

    ApplicationResponse response = applicationService.createApplicationForEmployee(createRequest(), employeeId, managerId);

    assertNotNull(response);
    assertEquals("APPROVED", response.status());
    verify(eventPublisher, times(2)).publishEvent(any(ApplicationNotificationEvent.class));
  }

  @Test
  void createApplicationForEmployee_ShouldCreateAndApprove_WhenUserIsHR() {
    Integer hrId = 1;
    Integer employeeId = 2;

    Role hrRole = new Role();
    hrRole.setName("ROLE_HR");

    User hrUser = new User();
    hrUser.setUserId(hrId);
    hrUser.setRoles(Set.of(hrRole));

    User employee = new User();
    employee.setUserId(employeeId);
    employee.setManager(null);

    Application savedApp = new Application();
    savedApp.setId(20L);
    savedApp.setEmployee(employee);
    savedApp.setType(ApplicationType.HOLIDAY_LEAVE);
    savedApp.setStatus(ApplicationStatus.PENDING);

    Application approvedApp = new Application();
    approvedApp.setId(20L);
    approvedApp.setEmployee(employee);
    approvedApp.setType(ApplicationType.HOLIDAY_LEAVE);
    approvedApp.setStatus(ApplicationStatus.APPROVED);

    when(userRepository.findById(hrId)).thenReturn(Optional.of(hrUser));
    when(userRepository.findById(employeeId)).thenReturn(Optional.of(employee));
    when(applicationRepository.save(any(Application.class))).thenReturn(savedApp).thenReturn(approvedApp);
    when(applicationRepository.findById(20L)).thenReturn(Optional.of(savedApp));

    ApplicationResponse response = applicationService.createApplicationForEmployee(createRequest(), employeeId, hrId);

    assertNotNull(response);
    assertEquals("APPROVED", response.status());
  }

  @Test
  void createApplicationForEmployee_ShouldThrowUnauthorized_WhenUserHasOtherRole() {
    User manager = new User();
    manager.setUserId(1);
    Role regularRole = new Role();
    regularRole.setName("ROLE_USER");
    manager.setRoles(Set.of(regularRole));

    User employee = new User();
    employee.setUserId(2);
    employee.setManager(null);

    when(userRepository.findById(1)).thenReturn(Optional.of(manager));
    when(userRepository.findById(2)).thenReturn(Optional.of(employee));

    assertThrows(ApplicationSecurityException.UnauthorizedCreation.class, () ->
        applicationService.createApplicationForEmployee(createRequest(), 2, 1)
    );
  }

  @Test
  void createApplicationForEmployee_ShouldCreate_WhenUserIsAdmin() {
    Integer adminId = 1;
    Integer employeeId = 2;

    Role adminRole = new Role();
    adminRole.setName("ROLE_ADMIN");

    User adminUser = new User();
    adminUser.setUserId(adminId);
    adminUser.setRoles(Set.of(adminRole));

    User employee = new User();
    employee.setUserId(employeeId);
    employee.setManager(null);

    Application savedApp = new Application();
    savedApp.setId(20L);
    savedApp.setEmployee(employee);
    savedApp.setType(ApplicationType.HOLIDAY_LEAVE);
    savedApp.setStatus(ApplicationStatus.PENDING);

    Application approvedApp = new Application();
    approvedApp.setId(20L);
    approvedApp.setEmployee(employee);
    approvedApp.setType(ApplicationType.HOLIDAY_LEAVE);
    approvedApp.setStatus(ApplicationStatus.APPROVED);

    when(userRepository.findById(adminId)).thenReturn(Optional.of(adminUser));
    when(userRepository.findById(employeeId)).thenReturn(Optional.of(employee));
    when(applicationRepository.save(any(Application.class))).thenReturn(savedApp).thenReturn(approvedApp);
    when(applicationRepository.findById(20L)).thenReturn(Optional.of(savedApp));

    ApplicationResponse response = applicationService.createApplicationForEmployee(createRequest(), employeeId, adminId);

    assertNotNull(response);
    assertEquals("APPROVED", response.status());
  }
}
