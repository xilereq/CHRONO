package pl.chrono.applicationmanager.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import pl.chrono.applicationmanager.dto.response.ApplicationResponse;
import pl.chrono.applicationmanager.exception.ApplicationSecurityException;
import pl.chrono.dbmanager.model.Application;
import pl.chrono.dbmanager.model.User;
import pl.chrono.dbmanager.model.enums.ApplicationStatus;
import pl.chrono.dbmanager.model.enums.ApplicationType;
import pl.chrono.dbmanager.repository.ApplicationRepository;
import pl.chrono.dbmanager.repository.UserRepository;
import pl.chrono.timemanager.service.ApplicationValidationService;
import pl.chrono.timemanager.service.OvertimeService;
import pl.chrono.usermanager.api.I_ManagerDelegationService;

@ExtendWith(MockitoExtension.class)
class DelegatedApplicationAccessServiceTest {

  private static final Integer MANAGER_ID = 2;
  private static final Integer DEPUTY_ID = 7;
  private static final Integer EMPLOYEE_ID = 11;

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
  @Mock
  private I_ManagerDelegationService delegationService;

  @InjectMocks
  private ApplicationService applicationService;

  private User user(Integer id, User manager) {
    User user = new User();
    user.setUserId(id);
    user.setName("Name" + id);
    user.setSurname("Surname" + id);
    user.setEmail("user" + id + "@chrono.pl");
    user.setManager(manager);
    return user;
  }

  private Application pendingApplication(Long id, User employee) {
    Application application = new Application();
    application.setId(id);
    application.setStatus(ApplicationStatus.PENDING);
    application.setEmployee(employee);
    application.setType(ApplicationType.HOLIDAY_LEAVE);
    application.setDurationSeconds(0);
    return application;
  }

  @Test
  void changeStatus_DeputyWithActiveDelegationCanApprove() {
    User manager = user(MANAGER_ID, null);
    User deputy = user(DEPUTY_ID, manager);
    Application application = pendingApplication(1L, user(EMPLOYEE_ID, manager));

    when(applicationRepository.findById(1L)).thenReturn(Optional.of(application));
    when(delegationService.isActiveDeputyOf(DEPUTY_ID, MANAGER_ID)).thenReturn(true);
    when(userRepository.findById(DEPUTY_ID)).thenReturn(Optional.of(deputy));
    when(applicationRepository.save(any(Application.class))).thenAnswer(inv -> inv.getArgument(0));

    ApplicationResponse response = applicationService.changeStatus(
        1L, "APPROVED", DEPUTY_ID, "Zastępstwo", false);

    assertEquals("APPROVED", response.status());
    assertEquals(DEPUTY_ID, response.reviewerId());
  }

  @Test
  void changeStatus_UserWithoutDelegationIsRejected() {
    User manager = user(MANAGER_ID, null);
    Application application = pendingApplication(1L, user(EMPLOYEE_ID, manager));

    when(applicationRepository.findById(1L)).thenReturn(Optional.of(application));
    when(delegationService.isActiveDeputyOf(DEPUTY_ID, MANAGER_ID)).thenReturn(false);

    assertThrows(ApplicationSecurityException.UnauthorizedManager.class,
        () -> applicationService.changeStatus(1L, "APPROVED", DEPUTY_ID, null, false));
    verify(applicationRepository, never()).save(any());
  }

  @Test
  void changeStatus_DeputyCannotApproveOwnApplication() {
    User manager = user(MANAGER_ID, null);
    Application ownApplication = pendingApplication(1L, user(DEPUTY_ID, manager));

    when(applicationRepository.findById(1L)).thenReturn(Optional.of(ownApplication));

    assertThrows(ApplicationSecurityException.UnauthorizedManager.class,
        () -> applicationService.changeStatus(1L, "APPROVED", DEPUTY_ID, null, false));
    verify(delegationService, never()).isActiveDeputyOf(any(), any());
    verify(applicationRepository, never()).save(any());
  }

  @Test
  void bulkApprove_DeputyApprovesTeamButNotOwnApplication() {
    User manager = user(MANAGER_ID, null);
    User deputy = user(DEPUTY_ID, manager);
    Application teamApplication = pendingApplication(1L, user(EMPLOYEE_ID, manager));
    Application ownApplication = pendingApplication(2L, deputy);

    when(userRepository.findById(DEPUTY_ID)).thenReturn(Optional.of(deputy));
    when(applicationRepository.findById(1L)).thenReturn(Optional.of(teamApplication));
    when(applicationRepository.findById(2L)).thenReturn(Optional.of(ownApplication));
    when(delegationService.isActiveDeputyOf(DEPUTY_ID, MANAGER_ID)).thenReturn(true);
    when(applicationRepository.save(any(Application.class))).thenAnswer(inv -> inv.getArgument(0));

    var result = applicationService.bulkApprove(List.of(1L, 2L), DEPUTY_ID, null, false);

    assertEquals(List.of(1L), result.processed().stream().map(ApplicationResponse::id).toList());
    assertEquals(2L, result.failed().get(0).applicationId());
    assertEquals(ApplicationStatus.PENDING, ownApplication.getStatus());
  }

  @Test
  void getPendingApplicationsForManager_IncludesSubstitutedTeamWithoutOwnApplications() {
    User manager = user(MANAGER_ID, null);
    User deputy = user(DEPUTY_ID, manager);
    User deputysOwnSubordinate = user(30, deputy);

    Application fromOwnTeam = pendingApplication(1L, deputysOwnSubordinate);
    Application fromSubstitutedTeam = pendingApplication(2L, user(EMPLOYEE_ID, manager));
    Application deputysOwn = pendingApplication(3L, deputy);

    when(applicationRepository.findPendingByManagerId(ApplicationStatus.PENDING, DEPUTY_ID))
        .thenReturn(List.of(fromOwnTeam));
    when(delegationService.getRepresentedManagerIds(DEPUTY_ID)).thenReturn(Set.of(MANAGER_ID));
    when(applicationRepository.findPendingByManagerId(ApplicationStatus.PENDING, MANAGER_ID))
        .thenReturn(List.of(fromSubstitutedTeam, deputysOwn));

    List<ApplicationResponse> result = applicationService.getPendingApplicationsForManager(DEPUTY_ID);

    assertEquals(List.of(1L, 2L), result.stream().map(ApplicationResponse::id).toList());
  }

  @Test
  void getUserApplications_DeputyCanViewSubstitutedEmployeeHistory() {
    User manager = user(MANAGER_ID, null);
    User employee = user(EMPLOYEE_ID, manager);
    User deputy = user(DEPUTY_ID, manager);

    when(userRepository.findById(EMPLOYEE_ID)).thenReturn(Optional.of(employee));
    when(userRepository.findById(DEPUTY_ID)).thenReturn(Optional.of(deputy));
    when(delegationService.isActiveDeputyOf(DEPUTY_ID, MANAGER_ID)).thenReturn(true);
    when(applicationRepository.findAllByEmployeeUserId(EMPLOYEE_ID))
        .thenReturn(List.of(pendingApplication(1L, employee)));

    List<ApplicationResponse> result = applicationService.getUserApplications(EMPLOYEE_ID, DEPUTY_ID);

    assertEquals(1, result.size());
  }
}
