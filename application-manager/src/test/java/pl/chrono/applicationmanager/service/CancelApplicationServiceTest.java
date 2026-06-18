package pl.chrono.applicationmanager.service;

import java.util.Optional;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import pl.chrono.applicationmanager.dto.response.ApplicationResponse;
import pl.chrono.applicationmanager.exception.ApplicationException;
import pl.chrono.applicationmanager.exception.ApplicationSecurityException;
import pl.chrono.dbmanager.model.Application;
import pl.chrono.dbmanager.model.User;
import pl.chrono.dbmanager.model.enums.ApplicationStatus;
import pl.chrono.dbmanager.model.enums.ApplicationType;
import pl.chrono.dbmanager.repository.ApplicationRepository;
import pl.chrono.timemanager.service.OvertimeService;

@ExtendWith(MockitoExtension.class)
class CancelApplicationServiceTest {

  @Mock
  private ApplicationRepository applicationRepository;

  @Mock
  private OvertimeService overtimeService;

  @InjectMocks
  private ApplicationService applicationService;

  @Test
  void cancelApplication_ShouldThrowNotFound_WhenApplicationDoesNotExist() {
    Long applicationId = 1L;
    when(applicationRepository.findById(applicationId)).thenReturn(Optional.empty());

    assertThrows(ApplicationException.NotFound.class, () ->
        applicationService.cancelApplication(applicationId, 1)
    );
  }

  @Test
  void cancelApplication_ShouldThrowUnauthorized_WhenUserIsNotOwner() {
    Long applicationId = 1L;
    User owner = new User();
    owner.setUserId(1);

    Application application = new Application();
    application.setId(applicationId);
    application.setEmployee(owner);

    when(applicationRepository.findById(applicationId)).thenReturn(Optional.of(application));

    assertThrows(ApplicationSecurityException.UnauthorizedCancellation.class, () ->
        applicationService.cancelApplication(applicationId, 2)
    );
  }

  @Test
  void cancelApplication_ShouldThrowNotPending_WhenStatusIsNotPending() {
    Long applicationId = 1L;
    Integer userId = 1;

    User owner = new User();
    owner.setUserId(userId);

    Application application = new Application();
    application.setId(applicationId);
    application.setEmployee(owner);
    application.setStatus(ApplicationStatus.APPROVED);

    when(applicationRepository.findById(applicationId)).thenReturn(Optional.of(application));

    assertThrows(ApplicationException.NotPending.class, () ->
        applicationService.cancelApplication(applicationId, userId)
    );
  }

  @Test
  void cancelApplication_ShouldCancelAndNotRefund_WhenTypeIsNotOvertimeTake() {
    Long applicationId = 1L;
    Integer userId = 1;

    User owner = new User();
    owner.setUserId(userId);

    Application application = new Application();
    application.setId(applicationId);
    application.setEmployee(owner);
    application.setStatus(ApplicationStatus.PENDING);
    application.setType(ApplicationType.HOLIDAY_LEAVE);

    when(applicationRepository.findById(applicationId)).thenReturn(Optional.of(application));
    when(applicationRepository.save(any(Application.class))).thenReturn(application);

    ApplicationResponse response = applicationService.cancelApplication(applicationId, userId);

    assertNotNull(response);
    assertEquals("CANCELLED", response.status());
    assertNotNull(response.decisionDateTime());
    verify(overtimeService, never()).refundOvertimeForApplication(any(Application.class));
    verify(applicationRepository, times(1)).save(application);
  }

  @Test
  void cancelApplication_ShouldCancelAndRefund_WhenTypeIsOvertimeTake() {
    Long applicationId = 1L;
    Integer userId = 1;

    User owner = new User();
    owner.setUserId(userId);

    Application application = new Application();
    application.setId(applicationId);
    application.setEmployee(owner);
    application.setStatus(ApplicationStatus.PENDING);
    application.setType(ApplicationType.OVERTIME_TAKE);

    when(applicationRepository.findById(applicationId)).thenReturn(Optional.of(application));
    when(applicationRepository.save(any(Application.class))).thenReturn(application);

    ApplicationResponse response = applicationService.cancelApplication(applicationId, userId);

    assertNotNull(response);
    assertEquals("CANCELLED", response.status());
    verify(overtimeService, times(1)).refundOvertimeForApplication(application);
    verify(applicationRepository, times(1)).save(application);
  }
}
