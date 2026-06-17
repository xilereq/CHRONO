package pl.chrono.timemanager.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import pl.chrono.dbmanager.model.Policy;
import pl.chrono.dbmanager.model.Role;
import pl.chrono.dbmanager.model.User;
import pl.chrono.dbmanager.model.WorkLog;
import pl.chrono.dbmanager.model.WorkLogRequest;
import pl.chrono.dbmanager.model.enums.WorkLocationType;
import pl.chrono.dbmanager.model.enums.WorkLogRequestStatus;
import pl.chrono.dbmanager.ports.I_TimeData;
import pl.chrono.dbmanager.repository.UserRepository;
import pl.chrono.timemanager.dto.request.WorkLogCreateRequest;
import pl.chrono.timemanager.dto.request.WorkLogResolveRequest;
import pl.chrono.timemanager.dto.response.WorkLogRequestDto;
import pl.chrono.timemanager.exception.UserException;
import pl.chrono.timemanager.exception.WorkLogRequestException;
import pl.chrono.timemanager.mapper.WorkLogRequestMapper;

@ExtendWith(MockitoExtension.class)
class WorkLogRequestServiceTest {

  private final Integer EMPLOYEE_ID = 5;
  private final Integer MANAGER_ID = 10;
  private final Long REQUEST_ID = 1L;
  @Mock
  private UserRepository userRepository;
  @Mock
  private I_TimeData timeData;
  @Mock
  private OvertimeService overtimeService;
  @Mock
  private WorkLogRequestMapper mapper;
  @Mock
  private WorkTimeCalculationService workTimeCalculationService;
  @InjectMocks
  private WorkLogRequestService requestService;

  @Test
  void submitRequestInvalidTimeRangeExceptionTest() {
    WorkLogCreateRequest req = mock(WorkLogCreateRequest.class);
    when(req.proposedStartTime()).thenReturn(LocalDateTime.now().plusHours(1));
    when(req.proposedEndTime()).thenReturn(LocalDateTime.now());

    assertThatThrownBy(() -> requestService.submitRequest(EMPLOYEE_ID, req)).isInstanceOf(
        WorkLogRequestException.InvalidTimeRange.class);
  }

  @Test
  void submitRequestSameStartSameEndExceptionTest() {
    LocalDateTime now = LocalDateTime.now();
    WorkLogCreateRequest req = mock(WorkLogCreateRequest.class);
    when(req.proposedStartTime()).thenReturn(now);
    when(req.proposedEndTime()).thenReturn(now);

    assertThatThrownBy(() -> requestService.submitRequest(EMPLOYEE_ID, req)).isInstanceOf(
        WorkLogRequestException.InvalidTimeRange.class);
  }

  @Test
  void submitRequestUserNotFoundExceptionTest() {
    WorkLogCreateRequest req = mock(WorkLogCreateRequest.class);
    when(req.proposedStartTime()).thenReturn(LocalDateTime.now());
    when(req.proposedEndTime()).thenReturn(LocalDateTime.now().plusHours(1));
    when(userRepository.findById(EMPLOYEE_ID)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> requestService.submitRequest(EMPLOYEE_ID, req)).isInstanceOf(
        UserException.NotFound.class);
  }

  @Test
  void submitRequestExceedsDailyLimitExceptionTest() {
    WorkLogCreateRequest req = mock(WorkLogCreateRequest.class);
    when(req.proposedStartTime()).thenReturn(LocalDateTime.now());
    when(req.proposedEndTime()).thenReturn(LocalDateTime.now().plusHours(14));

    User spyUser = spy(new User());
    Policy mockPolicy = mock(Policy.class);
    when(mockPolicy.getMaxAllowedSeconds()).thenReturn(12 * 3600);
    when(mockPolicy.getMaxDailyHours()).thenReturn(12);

    when(userRepository.findById(EMPLOYEE_ID)).thenReturn(Optional.of(spyUser));
    doReturn(mockPolicy).when(spyUser).getPolicyForDate(any());

    assertThatThrownBy(() -> requestService.submitRequest(EMPLOYEE_ID, req)).isInstanceOf(
        WorkLogRequestException.DailyLimitExceeded.class);
  }

  @Test
  void submitRequestInsufficientTimeExceptionTest() {
    WorkLogCreateRequest req = mock(WorkLogCreateRequest.class);
    when(req.proposedStartTime()).thenReturn(LocalDateTime.now());
    when(req.proposedEndTime()).thenReturn(LocalDateTime.now().plusHours(4));

    User spyUser = spy(new User());
    when(userRepository.findById(EMPLOYEE_ID)).thenReturn(Optional.of(spyUser));
    doReturn(null).when(spyUser).getPolicyForDate(any());

    when(workTimeCalculationService.getAdjustedRequiredSeconds(any(), any(), any())).thenReturn(
        8 * 3600);

    when(workTimeCalculationService.formatSecondsToTime(any(Long.class))).thenReturn("08:00:00");

    assertThatThrownBy(() -> requestService.submitRequest(EMPLOYEE_ID, req)).isInstanceOf(
        WorkLogRequestException.InsufficientTime.class).hasMessageContaining("08:00:00");
  }

  @Test
  void submitRequestSuccessTest() {
    WorkLogCreateRequest req = mock(WorkLogCreateRequest.class);
    when(req.proposedStartTime()).thenReturn(LocalDateTime.now());
    when(req.proposedEndTime()).thenReturn(LocalDateTime.now().plusHours(8));
    when(req.reason()).thenReturn("Zapomniałem kliknąć");

    User spyUser = spy(new User());
    when(userRepository.findById(EMPLOYEE_ID)).thenReturn(Optional.of(spyUser));
    doReturn(null).when(spyUser).getPolicyForDate(any());

    when(workTimeCalculationService.getAdjustedRequiredSeconds(any(), any(), any())).thenReturn(
        8 * 3600);

    requestService.submitRequest(EMPLOYEE_ID, req);

    ArgumentCaptor<WorkLogRequest> requestCaptor = ArgumentCaptor.forClass(WorkLogRequest.class);
    verify(timeData).saveWorkLogRequest(requestCaptor.capture());
    assertThat(requestCaptor.getValue().getStatus()).isEqualTo(WorkLogRequestStatus.PENDING);
    assertThat(requestCaptor.getValue().getReason()).isEqualTo("Zapomniałem kliknąć");
  }

  @Test
  void approveRequestUnauthorizedManagerExceptionTest() {
    User employee = new User();
    employee.setManager(null);

    WorkLogRequest request = new WorkLogRequest();
    request.setStatus(WorkLogRequestStatus.PENDING);
    request.setEmployee(employee);

    when(timeData.findWorkLogRequestById(REQUEST_ID)).thenReturn(Optional.of(request));

    User reviewer = new User();
    when(userRepository.findById(MANAGER_ID)).thenReturn(Optional.of(reviewer));

    assertThatThrownBy(
        () -> requestService.approveRequest(REQUEST_ID, MANAGER_ID, null)).isInstanceOf(
        WorkLogRequestException.UnauthorizedManager.class);
  }

  @Test
  void approveRequestWrongManagerTest() {
    User wrongManager = new User();
    wrongManager.setUserId(99);

    User employee = new User();
    employee.setManager(wrongManager);

    WorkLogRequest request = new WorkLogRequest();
    request.setStatus(WorkLogRequestStatus.PENDING);
    request.setEmployee(employee);

    when(timeData.findWorkLogRequestById(REQUEST_ID)).thenReturn(Optional.of(request));

    User reviewer = new User();
    reviewer.setUserId(MANAGER_ID);
    when(userRepository.findById(MANAGER_ID)).thenReturn(Optional.of(reviewer));

    assertThatThrownBy(
        () -> requestService.approveRequest(REQUEST_ID, MANAGER_ID, null)).isInstanceOf(
        WorkLogRequestException.UnauthorizedManager.class);
  }

  @Test
  void approveRequestAdminAccessTest() {
    User employee = new User();
    employee.setUserId(EMPLOYEE_ID);
    employee.setManager(null);

    WorkLogRequest request = new WorkLogRequest();
    request.setStatus(WorkLogRequestStatus.PENDING);
    request.setEmployee(employee);
    request.setProposedStartTime(LocalDateTime.now().minusHours(8));
    request.setProposedEndTime(LocalDateTime.now());

    when(timeData.findWorkLogRequestById(REQUEST_ID)).thenReturn(Optional.of(request));

    User admin = new User();
    Role adminRole = mock(Role.class);
    when(adminRole.getName()).thenReturn("ROLE_ADMIN");
    admin.setRoles(Set.of(adminRole));
    when(userRepository.findById(MANAGER_ID)).thenReturn(Optional.of(admin));

    when(timeData.findWorkLogsByUserAndDate(eq(EMPLOYEE_ID), any(), any())).thenReturn(List.of());

    requestService.approveRequest(REQUEST_ID, MANAGER_ID, new WorkLogResolveRequest("OK"));

    assertThat(request.getStatus()).isEqualTo(WorkLogRequestStatus.APPROVED);
    verify(timeData).saveWorkLogRequest(request);
  }

  @Test
  void approveRequestNoExistingLogsHomeOfficeCreationTest() {
    User manager = new User();
    manager.setUserId(MANAGER_ID);

    User employee = new User();
    employee.setUserId(EMPLOYEE_ID);
    employee.setManager(manager);

    WorkLogRequest request = new WorkLogRequest();
    request.setStatus(WorkLogRequestStatus.PENDING);
    request.setEmployee(employee);
    request.setProposedStartTime(LocalDateTime.now().minusHours(8));
    request.setProposedEndTime(LocalDateTime.now());

    when(timeData.findWorkLogRequestById(REQUEST_ID)).thenReturn(Optional.of(request));
    when(userRepository.findById(MANAGER_ID)).thenReturn(Optional.of(manager));

    when(timeData.findWorkLogsByUserAndDate(eq(EMPLOYEE_ID), any(), any())).thenReturn(List.of());
    when(workTimeCalculationService.hasApprovedHomeOffice(eq(EMPLOYEE_ID), any())).thenReturn(true);

    requestService.approveRequest(REQUEST_ID, MANAGER_ID, null);

    ArgumentCaptor<WorkLog> logCaptor = ArgumentCaptor.forClass(WorkLog.class);
    verify(timeData).saveWorkLog(logCaptor.capture());

    assertThat(logCaptor.getValue().getLocationType()).isEqualTo(WorkLocationType.HOME_OFFICE);
    assertThat(request.getManagerComment()).isNull();
  }

  @Test
  void approveRequestUpdatePrimaryLogTest() {
    User manager = new User();
    manager.setUserId(MANAGER_ID);

    User employee = new User();
    employee.setUserId(EMPLOYEE_ID);
    employee.setManager(manager);

    WorkLogRequest request = new WorkLogRequest();
    request.setStatus(WorkLogRequestStatus.PENDING);
    request.setEmployee(employee);
    request.setProposedStartTime(LocalDateTime.now().minusHours(8));
    request.setProposedEndTime(LocalDateTime.now());

    when(timeData.findWorkLogRequestById(REQUEST_ID)).thenReturn(Optional.of(request));
    when(userRepository.findById(MANAGER_ID)).thenReturn(Optional.of(manager));
    when(workTimeCalculationService.hasApprovedHomeOffice(any(), any())).thenReturn(false);

    WorkLog primaryLog = new WorkLog();
    primaryLog.setSettled(false);
    WorkLog extraLog1 = new WorkLog();
    WorkLog extraLog2 = new WorkLog();

    when(timeData.findWorkLogsByUserAndDate(eq(EMPLOYEE_ID), any(), any())).thenReturn(
        List.of(primaryLog, extraLog1, extraLog2));

    requestService.approveRequest(REQUEST_ID, MANAGER_ID, new WorkLogResolveRequest("OK"));

    verify(timeData).saveWorkLog(primaryLog);
    verify(timeData).deleteWorkLog(extraLog1);
    verify(timeData).deleteWorkLog(extraLog2);
    verify(overtimeService, never()).revertSettlementForWorkLog(any());
  }

  @Test
  void approveRequestSettlementReverseTest() {
    User manager = new User();
    manager.setUserId(MANAGER_ID);

    User employee = new User();
    employee.setUserId(EMPLOYEE_ID);
    employee.setManager(manager);

    WorkLogRequest request = new WorkLogRequest();
    request.setStatus(WorkLogRequestStatus.PENDING);
    request.setEmployee(employee);
    request.setProposedStartTime(LocalDateTime.now().minusHours(8));
    request.setProposedEndTime(LocalDateTime.now());

    when(timeData.findWorkLogRequestById(REQUEST_ID)).thenReturn(Optional.of(request));
    when(userRepository.findById(MANAGER_ID)).thenReturn(Optional.of(manager));

    WorkLog primaryLog = new WorkLog();
    primaryLog.setWorkLogId(99);
    primaryLog.setSettled(true);

    when(timeData.findWorkLogsByUserAndDate(eq(EMPLOYEE_ID), any(), any())).thenReturn(
        List.of(primaryLog));

    requestService.approveRequest(REQUEST_ID, MANAGER_ID, new WorkLogResolveRequest("Zgoda"));

    verify(overtimeService).revertSettlementForWorkLog(99);
    assertThat(primaryLog.isSettled()).isFalse();
    verify(timeData).saveWorkLog(primaryLog);
  }

  @Test
  void rejectRequestSuccessTest() {
    User manager = new User();
    manager.setUserId(MANAGER_ID);

    User employee = new User();
    employee.setUserId(EMPLOYEE_ID);
    employee.setManager(manager);

    WorkLogRequest request = new WorkLogRequest();
    request.setStatus(WorkLogRequestStatus.PENDING);
    request.setEmployee(employee);

    when(timeData.findWorkLogRequestById(REQUEST_ID)).thenReturn(Optional.of(request));
    when(userRepository.findById(MANAGER_ID)).thenReturn(Optional.of(manager));

    requestService.rejectRequest(REQUEST_ID, MANAGER_ID, new WorkLogResolveRequest("Brak zgody"));

    assertThat(request.getStatus()).isEqualTo(WorkLogRequestStatus.REJECTED);
    assertThat(request.getManagerComment()).isEqualTo("Brak zgody");
    verify(timeData).saveWorkLogRequest(request);
  }

  @Test
  void getPendingRequestInvalidStateExceptionTest() {
    WorkLogRequest request = new WorkLogRequest();
    request.setStatus(WorkLogRequestStatus.APPROVED);

    when(timeData.findWorkLogRequestById(REQUEST_ID)).thenReturn(Optional.of(request));

    assertThatThrownBy(
        () -> requestService.approveRequest(REQUEST_ID, MANAGER_ID, null)).isInstanceOf(
        WorkLogRequestException.InvalidState.class);
  }

  @Test
  void getEmployeeRequestsTest() {
    WorkLogRequest request = new WorkLogRequest();
    WorkLogRequestDto mockDto = mock(WorkLogRequestDto.class);

    when(timeData.findWorkLogRequestsByEmployee(EMPLOYEE_ID)).thenReturn(List.of(request));
    when(mapper.toDto(request)).thenReturn(mockDto);

    List<WorkLogRequestDto> result = requestService.getEmployeeRequests(EMPLOYEE_ID);

    assertThat(result).hasSize(1).contains(mockDto);
  }

  @Test
  void getPendingRequestsAdminTest() {
    User admin = new User();
    Role adminRole = mock(Role.class);
    when(adminRole.getName()).thenReturn("ROLE_ADMIN");
    admin.setRoles(Set.of(adminRole));

    when(userRepository.findById(MANAGER_ID)).thenReturn(Optional.of(admin));
    when(timeData.findAllPendingWorkLogRequests()).thenReturn(List.of(new WorkLogRequest()));
    when(mapper.toDto(any())).thenReturn(mock(WorkLogRequestDto.class));

    List<WorkLogRequestDto> result = requestService.getPendingRequestsForReviewer(MANAGER_ID);

    assertThat(result).hasSize(1);
    verify(timeData).findAllPendingWorkLogRequests();
  }

  @Test
  void getPendingRequestsManagerTest() {
    User manager = new User();
    when(userRepository.findById(MANAGER_ID)).thenReturn(Optional.of(manager));
    when(timeData.findPendingWorkLogRequestsForManager(MANAGER_ID)).thenReturn(
        List.of(new WorkLogRequest()));
    when(mapper.toDto(any())).thenReturn(mock(WorkLogRequestDto.class));

    List<WorkLogRequestDto> result = requestService.getPendingRequestsForReviewer(MANAGER_ID);

    assertThat(result).hasSize(1);
    verify(timeData).findPendingWorkLogRequestsForManager(MANAGER_ID);
  }

  @Test
  void getPendingRequestsForReviewerUserNotFoundExceptionTest() {
    when(userRepository.findById(MANAGER_ID)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> requestService.getPendingRequestsForReviewer(MANAGER_ID)).isInstanceOf(
        UserException.NotFound.class);
  }

  @Test
  void approveRequestRequestNotFoundExceptionTes() {
    when(timeData.findWorkLogRequestById(REQUEST_ID)).thenReturn(Optional.empty());

    assertThatThrownBy(
        () -> requestService.approveRequest(REQUEST_ID, MANAGER_ID, null)).isInstanceOf(
        WorkLogRequestException.NotFound.class);
  }

  @Test
  void approveRequestReviewerNotFoundExceptionTest() {
    WorkLogRequest request = new WorkLogRequest();
    request.setStatus(WorkLogRequestStatus.PENDING);

    when(timeData.findWorkLogRequestById(REQUEST_ID)).thenReturn(Optional.of(request));
    when(userRepository.findById(MANAGER_ID)).thenReturn(Optional.empty());

    assertThatThrownBy(
        () -> requestService.approveRequest(REQUEST_ID, MANAGER_ID, null)).isInstanceOf(
        UserException.NotFound.class);
  }

  @Test
  void rejectRequestNullResolveDtoTest() {
    User manager = new User();
    manager.setUserId(MANAGER_ID);

    User employee = new User();
    employee.setUserId(EMPLOYEE_ID);
    employee.setManager(manager);

    WorkLogRequest request = new WorkLogRequest();
    request.setStatus(WorkLogRequestStatus.PENDING);
    request.setEmployee(employee);

    when(timeData.findWorkLogRequestById(REQUEST_ID)).thenReturn(Optional.of(request));
    when(userRepository.findById(MANAGER_ID)).thenReturn(Optional.of(manager));

    requestService.rejectRequest(REQUEST_ID, MANAGER_ID, null);

    assertThat(request.getStatus()).isEqualTo(WorkLogRequestStatus.REJECTED);
    assertThat(request.getManagerComment()).isNull();
    verify(timeData).saveWorkLogRequest(request);
  }

  @Test
  void submitRequestWithNullPolicyTest() {
    WorkLogCreateRequest req = mock(WorkLogCreateRequest.class);
    when(req.proposedStartTime()).thenReturn(LocalDateTime.now());
    when(req.proposedEndTime()).thenReturn(LocalDateTime.now().plusHours(8));
    when(req.reason()).thenReturn("Test limits");

    User spyUser = spy(new User());
    when(userRepository.findById(EMPLOYEE_ID)).thenReturn(Optional.of(spyUser));

    Policy mockPolicy = mock(Policy.class);
    when(mockPolicy.getMaxAllowedSeconds()).thenReturn(12 * 3600);

    doReturn(mockPolicy).when(spyUser).getPolicyForDate(any());

    when(workTimeCalculationService.getAdjustedRequiredSeconds(any(), any(), any())).thenReturn(
        8 * 3600);

    requestService.submitRequest(EMPLOYEE_ID, req);

    ArgumentCaptor<WorkLogRequest> requestCaptor = ArgumentCaptor.forClass(WorkLogRequest.class);
    verify(timeData).saveWorkLogRequest(requestCaptor.capture());
    assertThat(requestCaptor.getValue().getStatus()).isEqualTo(WorkLogRequestStatus.PENDING);
  }
}
