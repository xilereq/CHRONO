package pl.chrono.timemanager.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import pl.chrono.dbmanager.model.User;
import pl.chrono.dbmanager.model.WorkLog;
import pl.chrono.dbmanager.model.WorkLogRequest;
import pl.chrono.dbmanager.model.enums.WorkLogRequestStatus;
import pl.chrono.dbmanager.model.enums.WorkLogRequestType;
import pl.chrono.dbmanager.ports.I_TimeData;
import pl.chrono.dbmanager.repository.UserRepository;
import pl.chrono.timemanager.dto.request.WorkLogResolveRequest;
import pl.chrono.timemanager.dto.response.WorkLogRequestDto;
import pl.chrono.timemanager.exception.WorkLogRequestException;
import pl.chrono.timemanager.mapper.WorkLogRequestMapper;

@ExtendWith(MockitoExtension.class)
class WorkLogRequestServiceTest {

  @Mock
  private UserRepository userRepository;

  @Mock
  private I_TimeData timeData;

  @Mock
  private WorkLogRequestMapper mapper;

  @InjectMocks
  private WorkLogRequestService requestService;

  @Test
  void rejectRequestTest() {
    Long requestId = 1L;
    Integer managerId = 10;
    WorkLogResolveRequest resolveDto = new WorkLogResolveRequest("Brak zgody");

    User manager = new User();
    manager.setUserId(managerId);

    User employee = new User();
    employee.setManager(manager);

    WorkLogRequest request = new WorkLogRequest();
    request.setStatus(WorkLogRequestStatus.PENDING);
    request.setEmployee(employee);

    when(timeData.findWorkLogRequestById(requestId)).thenReturn(Optional.of(request));
    when(userRepository.findById(managerId)).thenReturn(Optional.of(manager));

    requestService.rejectRequest(requestId, managerId, resolveDto);

    assertEquals(WorkLogRequestStatus.REJECTED, request.getStatus());
    assertEquals("Brak zgody", request.getManagerComment());
    assertNotNull(request.getResolvedAt());
    verify(timeData).saveWorkLogRequest(request);
  }

  @Test
  void approveRequestTypeUpdateTest() {
    Long requestId = 1L;
    Integer managerId = 10;
    WorkLogResolveRequest resolveDto = new WorkLogResolveRequest("OK");

    User manager = new User();
    manager.setUserId(managerId);

    User employee = new User();
    employee.setManager(manager);

    WorkLog targetLog = new WorkLog();

    WorkLogRequest request = new WorkLogRequest();
    request.setStatus(WorkLogRequestStatus.PENDING);
    request.setType(WorkLogRequestType.UPDATE);
    request.setEmployee(employee);
    request.setTargetWorkLog(targetLog);
    request.setProposedStartTime(LocalDateTime.now().minusHours(8));
    request.setProposedEndTime(LocalDateTime.now());

    when(timeData.findWorkLogRequestById(requestId)).thenReturn(Optional.of(request));
    when(userRepository.findById(managerId)).thenReturn(Optional.of(manager));

    requestService.approveRequest(requestId, managerId, resolveDto);

    assertEquals(WorkLogRequestStatus.APPROVED, request.getStatus());
    assertEquals("OK", request.getManagerComment());
    verify(timeData).saveWorkLog(targetLog);
    verify(timeData).saveWorkLogRequest(request);
  }

  @Test
  void approveRequestUnauthorizedManagerExceptionTest() {
    Long requestId = 1L;
    Integer invalidManagerId = 99;

    User manager = new User();
    manager.setUserId(10);

    User employee = new User();
    employee.setManager(manager);

    WorkLogRequest request = new WorkLogRequest();
    request.setStatus(WorkLogRequestStatus.PENDING);
    request.setEmployee(employee);

    when(timeData.findWorkLogRequestById(requestId)).thenReturn(Optional.of(request));

    assertThrows(WorkLogRequestException.UnauthorizedManager.class, () ->
        requestService.approveRequest(requestId, invalidManagerId, new WorkLogResolveRequest("OK"))
    );

    verifyNoInteractions(userRepository);
    verify(timeData, never()).saveWorkLogRequest(any());
  }

  @Test
  void adminApproveRequestBypassesManagerCheckTest() {
    Long requestId = 1L;
    Integer adminId = 999;
    WorkLogResolveRequest resolveDto = new WorkLogResolveRequest("Admin Override");

    User employee = new User();

    User admin = new User();
    admin.setUserId(adminId);

    WorkLogRequest request = new WorkLogRequest();
    request.setStatus(WorkLogRequestStatus.PENDING);
    request.setType(WorkLogRequestType.CREATE);
    request.setEmployee(employee);
    request.setProposedStartTime(LocalDateTime.now().minusHours(8));
    request.setProposedEndTime(LocalDateTime.now());

    when(timeData.findWorkLogRequestById(requestId)).thenReturn(Optional.of(request));
    when(userRepository.findById(adminId)).thenReturn(Optional.of(admin));

    requestService.adminApproveRequest(requestId, adminId, resolveDto);

    assertEquals(WorkLogRequestStatus.APPROVED, request.getStatus());
    assertEquals("Admin Override", request.getManagerComment());
    verify(timeData).saveWorkLog(any(WorkLog.class));
    verify(timeData).saveWorkLogRequest(request);
  }

  @Test
  void getPendingRequestInvalidStateExceptionTest() {
    Long requestId = 1L;
    WorkLogRequest request = new WorkLogRequest();
    request.setStatus(WorkLogRequestStatus.APPROVED);

    when(timeData.findWorkLogRequestById(requestId)).thenReturn(Optional.of(request));

    assertThrows(WorkLogRequestException.InvalidState.class, () ->
        requestService.approveRequest(requestId, 10, null)
    );
  }

  @Test
  void getEmployeeRequestsTest() {
    Integer employeeId = 5;
    WorkLogRequest request = new WorkLogRequest();
    WorkLogRequestDto mockDto = mock(WorkLogRequestDto.class);

    when(timeData.findWorkLogRequestsByEmployee(employeeId)).thenReturn(List.of(request));
    when(mapper.toDto(request)).thenReturn(mockDto);

    List<WorkLogRequestDto> result = requestService.getEmployeeRequests(employeeId);

    assertEquals(1, result.size());
    assertTrue(result.contains(mockDto));
    verify(timeData).findWorkLogRequestsByEmployee(employeeId);
    verify(mapper).toDto(request);
  }

  @Test
  void getAllPendingRequestsTest() {
    WorkLogRequest req1 = new WorkLogRequest();
    WorkLogRequest req2 = new WorkLogRequest();
    WorkLogRequestDto mockDto1 = mock(WorkLogRequestDto.class);
    WorkLogRequestDto mockDto2 = mock(WorkLogRequestDto.class);

    when(timeData.findAllPendingWorkLogRequests()).thenReturn(List.of(req1, req2));
    when(mapper.toDto(req1)).thenReturn(mockDto1);
    when(mapper.toDto(req2)).thenReturn(mockDto2);

    List<WorkLogRequestDto> result = requestService.getAllPendingRequests();

    assertEquals(2, result.size());
    verify(timeData).findAllPendingWorkLogRequests();
    verify(mapper, times(2)).toDto(any(WorkLogRequest.class));
  }
}
