package pl.chrono.applicationmanager.service;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import pl.chrono.applicationmanager.dto.response.ApplicationResponse;
import pl.chrono.applicationmanager.exception.ApplicationSecurityException;
import pl.chrono.applicationmanager.exception.ApplicationUserException;
import pl.chrono.dbmanager.model.Application;
import pl.chrono.dbmanager.model.Role;
import pl.chrono.dbmanager.model.User;
import pl.chrono.dbmanager.model.enums.ApplicationStatus;
import pl.chrono.dbmanager.model.enums.ApplicationType;
import pl.chrono.dbmanager.repository.ApplicationRepository;
import pl.chrono.dbmanager.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class GetApplicationServiceTest {

  @Mock
  private ApplicationRepository applicationRepository;

  @Mock
  private UserRepository userRepository;

  @InjectMocks
  private ApplicationService applicationService;

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
  void getUserApplications_ShouldThrowException_WhenRequesterHasOtherRoleAndIsNotManager() {
    Integer employeeId = 1;
    Integer requesterId = 2;

    User otherManager = new User();
    otherManager.setUserId(3);

    User mockEmployee = new User();
    mockEmployee.setUserId(employeeId);
    mockEmployee.setManager(otherManager);

    Role regularRole = new Role();
    regularRole.setName("ROLE_USER");

    User mockRequester = new User();
    mockRequester.setUserId(requesterId);
    mockRequester.setRoles(Set.of(regularRole));

    when(userRepository.findById(employeeId)).thenReturn(Optional.of(mockEmployee));
    when(userRepository.findById(requesterId)).thenReturn(Optional.of(mockRequester));

    assertThrows(ApplicationSecurityException.UnauthorizedAccess.class, () ->
        applicationService.getUserApplications(employeeId, requesterId)
    );
  }
}
