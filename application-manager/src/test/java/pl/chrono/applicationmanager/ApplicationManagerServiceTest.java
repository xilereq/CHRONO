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
import pl.chrono.applicationmanager.dto.ApplicationDTO;
import pl.chrono.applicationmanager.service.ApplicationService;
import pl.chrono.dbmanager.model.Application;
import pl.chrono.dbmanager.model.User;
import pl.chrono.dbmanager.model.Role;
import pl.chrono.dbmanager.model.enums.ApplicationStatus;
import pl.chrono.dbmanager.model.enums.ApplicationType;
import pl.chrono.dbmanager.repository.ApplicationRepository;
import pl.chrono.dbmanager.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class ApplicationServiceTest {

  @Mock
  private ApplicationRepository applicationRepository;

  @Mock
  private UserRepository userRepository;

  @InjectMocks
  private ApplicationService applicationService;

  @Test
  void createApplication_ShouldReturnDto_WhenUserExists() {
    Integer employeeId = 1;

    User mockUser = new User();
    mockUser.setUserId(employeeId);

    ApplicationDTO requestDto = new ApplicationDTO();
    requestDto.setType("HOLIDAY_LEAVE");
    requestDto.setStartDate(LocalDate.of(2026, 5, 1));
    requestDto.setEndDate(LocalDate.of(2026, 5, 10));
    requestDto.setReason("Majówka");

    Application savedApplication = new Application();
    savedApplication.setId(100L);
    savedApplication.setEmployee(mockUser);
    savedApplication.setType(ApplicationType.HOLIDAY_LEAVE);
    savedApplication.setStatus(ApplicationStatus.PENDING);
    savedApplication.setStartDate(requestDto.getStartDate());
    savedApplication.setEndDate(requestDto.getEndDate());
    savedApplication.setReason(requestDto.getReason());
    savedApplication.setCreationDateTime(java.time.LocalDateTime.now());

    when(userRepository.findById(employeeId)).thenReturn(Optional.of(mockUser));
    when(applicationRepository.save(any(Application.class))).thenReturn(savedApplication);

    ApplicationDTO result = applicationService.createApplication(requestDto, employeeId);

    assertNotNull(result);
    assertEquals(100L, result.getId());
    assertEquals("HOLIDAY_LEAVE", result.getType());
    assertEquals("PENDING", result.getStatus());
    assertEquals(employeeId, result.getEmployeeId());
    assertNotNull(result.getCreationDateTime());

    verify(applicationRepository, times(1)).save(any(Application.class));
  }

  @Test
  void changeStatus_ShouldUpdateStatusAndReviewer_WhenBothExist() {
    Long applicationId = 1L;
    Integer reviewerId = 2;
    String newStatus = "APPROVED";
    String decisionReason = "Valid reason";

    User mockReviewer = new User();
    mockReviewer.setUserId(reviewerId);

    User mockEmployee = new User();
    mockEmployee.setUserId(1);
    mockEmployee.setManager(mockReviewer);

    Application mockApplication = new Application();
    mockApplication.setId(applicationId);
    mockApplication.setStatus(ApplicationStatus.PENDING);
    mockApplication.setEmployee(mockEmployee);
    mockApplication.setType(ApplicationType.HOLIDAY_LEAVE);

    when(applicationRepository.findById(applicationId)).thenReturn(Optional.of(mockApplication));
    when(userRepository.findById(reviewerId)).thenReturn(Optional.of(mockReviewer));
    when(applicationRepository.save(any(Application.class))).thenReturn(mockApplication);

    ApplicationDTO result = applicationService.changeStatus(applicationId, newStatus, reviewerId, decisionReason);

    assertNotNull(result);
    assertEquals(newStatus, result.getStatus());
    assertEquals(decisionReason, result.getDecisionReason());
    assertEquals(reviewerId, result.getReviewerId());
    assertNotNull(result.getDecisionDateTime());
    verify(applicationRepository, times(1)).save(mockApplication);
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

    List<ApplicationDTO> result = applicationService.getUserApplications(employeeId, requesterId);

    assertNotNull(result);
    assertEquals(1, result.size());
    assertEquals(10L, result.getFirst().getId());
    assertEquals("SICK_LEAVE", result.getFirst().getType());
  }

  @Test
  void getPendingApplicationsForManager_ShouldReturnListOfDtos() {
    Integer managerId = 2;

    User mockEmployee = new User();
    mockEmployee.setUserId(1);

    Application mockApplication = new Application();
    mockApplication.setId(20L);
    mockApplication.setEmployee(mockEmployee);
    mockApplication.setType(ApplicationType.HOLIDAY_LEAVE);
    mockApplication.setStatus(ApplicationStatus.PENDING);

    when(applicationRepository.findPendingByManagerId(ApplicationStatus.PENDING, managerId))
        .thenReturn(List.of(mockApplication));

    List<ApplicationDTO> result = applicationService.getPendingApplicationsForManager(managerId);

    assertNotNull(result);
    assertEquals(1, result.size());
    assertEquals(20L, result.getFirst().getId());
    assertEquals("PENDING", result.getFirst().getStatus());

    verify(applicationRepository, times(1)).findPendingByManagerId(ApplicationStatus.PENDING, managerId);
  }

  @Test
  void createApplication_ShouldThrowException_WhenUserDoesNotExist() {
    Integer employeeId = 999;
    ApplicationDTO requestDto = new ApplicationDTO();

    when(userRepository.findById(employeeId)).thenReturn(Optional.empty());

    RuntimeException exception = assertThrows(RuntimeException.class, () ->
        applicationService.createApplication(requestDto, employeeId)
    );
    assertEquals("Nie znaleziono pracownika o ID: 999", exception.getMessage());
  }

  @Test
  void changeStatus_ShouldThrowException_WhenApplicationNotPending() {
    Long applicationId = 1L;
    Application mockApplication = new Application();
    mockApplication.setId(applicationId);
    mockApplication.setStatus(ApplicationStatus.APPROVED);

    when(applicationRepository.findById(applicationId)).thenReturn(Optional.of(mockApplication));

    IllegalStateException exception = assertThrows(IllegalStateException.class, () ->
        applicationService.changeStatus(applicationId, "REJECTED", 2, "Reason")
    );
    assertNotNull(exception.getMessage());
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

    SecurityException exception = assertThrows(SecurityException.class, () ->
        applicationService.changeStatus(applicationId, "APPROVED", 2, "Reason")
    );
    assertEquals("Brak uprawnień. Tylko bezpośredni przełożony może rozpatrywać wnioski tego pracownika.", exception.getMessage());
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

    SecurityException exception = assertThrows(SecurityException.class, () ->
        applicationService.changeStatus(applicationId, "APPROVED", fakeReviewerId, "Reason")
    );
    assertNotNull(exception.getMessage());
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

    SecurityException exception = assertThrows(SecurityException.class, () ->
        applicationService.getUserApplications(employeeId, requesterId)
    );
    assertNotNull(exception.getMessage());
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

    List<ApplicationDTO> result = applicationService.getUserApplications(employeeId, requesterId);

    assertNotNull(result);
    assertEquals(1, result.size());
  }
}
