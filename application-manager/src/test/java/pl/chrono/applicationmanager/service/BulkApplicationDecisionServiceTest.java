package pl.chrono.applicationmanager.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import pl.chrono.applicationmanager.dto.response.BulkApplicationDecisionResponse;
import pl.chrono.applicationmanager.event.ApplicationNotificationEvent;
import pl.chrono.applicationmanager.exception.ApplicationUserException;
import pl.chrono.applicationmanager.exception.ErrorCodes;
import pl.chrono.dbmanager.model.Application;
import pl.chrono.dbmanager.model.User;
import pl.chrono.dbmanager.model.enums.ApplicationStatus;
import pl.chrono.dbmanager.model.enums.ApplicationType;
import pl.chrono.dbmanager.repository.ApplicationRepository;
import pl.chrono.dbmanager.repository.UserRepository;
import pl.chrono.timemanager.service.ApplicationValidationService;
import pl.chrono.timemanager.service.OvertimeService;

@ExtendWith(MockitoExtension.class)
class BulkApplicationDecisionServiceTest {

  private static final Integer MANAGER_ID = 2;
  private static final Integer OTHER_MANAGER_ID = 3;

  @Mock
  private ApplicationRepository applicationRepository;

  @Mock
  private UserRepository userRepository;

  @Mock
  private ApplicationEventPublisher eventPublisher;

  @Mock
  private ApplicationValidationService applicationValidationService;

  @Mock
  private OvertimeService overtimeService;

  @InjectMocks
  private ApplicationService applicationService;

  private User manager(Integer id) {
    User manager = new User();
    manager.setUserId(id);
    manager.setEmail("manager" + id + "@chrono.pl");
    manager.setName("Jan");
    manager.setSurname("Kowalski");
    return manager;
  }

  private Application application(Long id, ApplicationStatus status, User employeeManager) {
    User employee = new User();
    employee.setUserId(100 + id.intValue());
    employee.setEmail("employee" + id + "@chrono.pl");
    employee.setManager(employeeManager);

    Application application = new Application();
    application.setId(id);
    application.setStatus(status);
    application.setEmployee(employee);
    application.setType(ApplicationType.HOLIDAY_LEAVE);
    application.setDurationSeconds(0);
    return application;
  }

  @Test
  void bulkApprove_ShouldApproveValidAndReportInvalid() {
    User reviewer = manager(MANAGER_ID);
    Application ok1 = application(1L, ApplicationStatus.PENDING, reviewer);
    Application ok2 = application(2L, ApplicationStatus.PENDING, reviewer);
    Application alreadyApproved = application(3L, ApplicationStatus.APPROVED, reviewer);
    Application otherTeam = application(4L, ApplicationStatus.PENDING, manager(OTHER_MANAGER_ID));

    when(userRepository.findById(MANAGER_ID)).thenReturn(Optional.of(reviewer));
    when(applicationRepository.findById(1L)).thenReturn(Optional.of(ok1));
    when(applicationRepository.findById(2L)).thenReturn(Optional.of(ok2));
    when(applicationRepository.findById(3L)).thenReturn(Optional.of(alreadyApproved));
    when(applicationRepository.findById(4L)).thenReturn(Optional.of(otherTeam));
    when(applicationRepository.findById(5L)).thenReturn(Optional.empty());
    when(applicationRepository.save(any(Application.class))).thenAnswer(inv -> inv.getArgument(0));

    BulkApplicationDecisionResponse result = applicationService.bulkApprove(
        List.of(1L, 2L, 3L, 4L, 5L), MANAGER_ID, "OK", false);

    assertEquals(List.of(1L, 2L), result.processed().stream().map(r -> r.id()).toList());
    assertTrue(result.processed().stream().allMatch(r -> r.status().equals("APPROVED")));

    assertEquals(3, result.failed().size());
    assertEquals(ErrorCodes.APPLICATION_ALREADY_PROCESSED, result.failed().get(0).errorCode());
    assertEquals(4L, result.failed().get(1).applicationId());
    assertEquals(ErrorCodes.UNAUTHORIZED_MANAGER, result.failed().get(1).errorCode());
    assertEquals(ErrorCodes.APPLICATION_NOT_FOUND, result.failed().get(2).errorCode());

    assertEquals(ApplicationStatus.PENDING, otherTeam.getStatus());
    verify(applicationRepository, times(2)).save(any(Application.class));
    verify(eventPublisher, times(2)).publishEvent(any(ApplicationNotificationEvent.class));
  }

  @Test
  void bulkApprove_ShouldIgnoreDuplicateIds() {
    User reviewer = manager(MANAGER_ID);
    Application app = application(1L, ApplicationStatus.PENDING, reviewer);

    when(userRepository.findById(MANAGER_ID)).thenReturn(Optional.of(reviewer));
    when(applicationRepository.findById(1L)).thenReturn(Optional.of(app));
    when(applicationRepository.save(any(Application.class))).thenAnswer(inv -> inv.getArgument(0));

    BulkApplicationDecisionResponse result = applicationService.bulkApprove(
        List.of(1L, 1L), MANAGER_ID, null, false);

    assertEquals(1, result.processed().size());
    assertTrue(result.failed().isEmpty());
    verify(applicationRepository, times(1)).save(any(Application.class));
  }

  @Test
  void bulkApprove_AdminCanApproveApplicationsOfAnyTeam() {
    User admin = manager(99);
    Application otherTeam = application(1L, ApplicationStatus.PENDING, manager(OTHER_MANAGER_ID));

    when(userRepository.findById(99)).thenReturn(Optional.of(admin));
    when(applicationRepository.findById(1L)).thenReturn(Optional.of(otherTeam));
    when(applicationRepository.save(any(Application.class))).thenAnswer(inv -> inv.getArgument(0));

    BulkApplicationDecisionResponse result = applicationService.bulkApprove(
        List.of(1L), 99, null, true);

    assertEquals(1, result.processed().size());
    assertEquals(ApplicationStatus.APPROVED, otherTeam.getStatus());
  }

  @Test
  void bulkApprove_ShouldThrow_WhenReviewerDoesNotExist() {
    when(userRepository.findById(MANAGER_ID)).thenReturn(Optional.empty());

    assertThrows(ApplicationUserException.NotFound.class,
        () -> applicationService.bulkApprove(List.of(1L), MANAGER_ID, null, false));

    verify(applicationRepository, never()).save(any(Application.class));
  }

  @Test
  void bulkReject_ShouldRejectValidAndReportInvalid() {
    User reviewer = manager(MANAGER_ID);
    Application ok = application(1L, ApplicationStatus.PENDING, reviewer);
    Application alreadyRejected = application(2L, ApplicationStatus.REJECTED, reviewer);
    Application otherTeam = application(3L, ApplicationStatus.PENDING, manager(OTHER_MANAGER_ID));

    when(userRepository.findById(MANAGER_ID)).thenReturn(Optional.of(reviewer));
    when(applicationRepository.findById(1L)).thenReturn(Optional.of(ok));
    when(applicationRepository.findById(2L)).thenReturn(Optional.of(alreadyRejected));
    when(applicationRepository.findById(3L)).thenReturn(Optional.of(otherTeam));
    when(applicationRepository.save(any(Application.class))).thenAnswer(inv -> inv.getArgument(0));

    BulkApplicationDecisionResponse result = applicationService.bulkReject(
        List.of(1L, 2L, 3L), MANAGER_ID, "Brak zastępstwa", false);

    assertEquals(1, result.processed().size());
    assertEquals("REJECTED", result.processed().get(0).status());
    assertEquals("Brak zastępstwa", result.processed().get(0).decisionReason());
    assertEquals(ApplicationStatus.REJECTED, ok.getStatus());

    assertEquals(2, result.failed().size());
    assertEquals(ErrorCodes.APPLICATION_ALREADY_PROCESSED, result.failed().get(0).errorCode());
    assertEquals(ErrorCodes.UNAUTHORIZED_MANAGER, result.failed().get(1).errorCode());
    assertEquals(ApplicationStatus.PENDING, otherTeam.getStatus());

    verify(eventPublisher, times(1)).publishEvent(any(ApplicationNotificationEvent.class));
    verify(overtimeService, never()).refundOvertimeForApplication(any());
  }

  @Test
  void bulkReject_ShouldRefundOvertime_ForOvertimeTakeApplications() {
    User reviewer = manager(MANAGER_ID);
    Application overtimeTake = application(1L, ApplicationStatus.PENDING, reviewer);
    overtimeTake.setType(ApplicationType.OVERTIME_TAKE);
    overtimeTake.setDurationSeconds(3600);

    when(userRepository.findById(MANAGER_ID)).thenReturn(Optional.of(reviewer));
    when(applicationRepository.findById(1L)).thenReturn(Optional.of(overtimeTake));
    when(applicationRepository.save(any(Application.class))).thenAnswer(inv -> inv.getArgument(0));

    applicationService.bulkReject(List.of(1L), MANAGER_ID, null, false);

    verify(overtimeService, times(1)).refundOvertimeForApplication(overtimeTake);
  }

  @Test
  void bulkApprove_ShouldNotRefundOvertime_ForOvertimeTakeApplications() {
    User reviewer = manager(MANAGER_ID);
    Application overtimeTake = application(1L, ApplicationStatus.PENDING, reviewer);
    overtimeTake.setType(ApplicationType.OVERTIME_TAKE);

    when(userRepository.findById(MANAGER_ID)).thenReturn(Optional.of(reviewer));
    when(applicationRepository.findById(1L)).thenReturn(Optional.of(overtimeTake));
    when(applicationRepository.save(any(Application.class))).thenAnswer(inv -> inv.getArgument(0));

    applicationService.bulkApprove(List.of(1L), MANAGER_ID, null, false);

    verify(overtimeService, never()).refundOvertimeForApplication(any());
  }
}
