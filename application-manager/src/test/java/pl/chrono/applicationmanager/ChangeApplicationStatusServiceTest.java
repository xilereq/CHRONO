package pl.chrono.applicationmanager;

import java.util.Optional;
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

import pl.chrono.applicationmanager.dto.response.ApplicationResponse;
import pl.chrono.applicationmanager.event.ApplicationNotificationEvent;
import pl.chrono.applicationmanager.exception.ApplicationException;
import pl.chrono.applicationmanager.exception.ApplicationSecurityException;
import pl.chrono.applicationmanager.exception.ApplicationUserException;
import pl.chrono.applicationmanager.service.ApplicationService;
import pl.chrono.dbmanager.model.Application;
import pl.chrono.dbmanager.model.User;
import pl.chrono.dbmanager.model.enums.ApplicationStatus;
import pl.chrono.dbmanager.model.enums.ApplicationType;
import pl.chrono.dbmanager.repository.ApplicationRepository;
import pl.chrono.dbmanager.repository.UserRepository;
import pl.chrono.timemanager.service.ApplicationValidationService;

@ExtendWith(MockitoExtension.class)
class ChangeApplicationStatusServiceTest {

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
