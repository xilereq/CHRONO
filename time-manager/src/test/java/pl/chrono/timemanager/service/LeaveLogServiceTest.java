package pl.chrono.timemanager.service;

import java.util.Optional;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import pl.chrono.dbmanager.model.LeaveLog;
import pl.chrono.dbmanager.model.User;
import pl.chrono.dbmanager.model.WorkLog;
import pl.chrono.dbmanager.ports.I_TimeData;
import pl.chrono.dbmanager.repository.UserRepository;
import pl.chrono.timemanager.dto.response.LeaveLogDto;
import pl.chrono.timemanager.exception.WorkException;
import pl.chrono.timemanager.mapper.LeaveLogMapper;

@ExtendWith(MockitoExtension.class)
class LeaveLogServiceTest {

  @Mock
  private I_TimeData timeData;

  @Mock
  private UserRepository userRepository;

  @Mock
  private LeaveLogMapper leaveLogMapper;

  @InjectMocks
  private LeaveLogService leaveLogService;

  @Test
  void startLeaveNotWorkingExceptionTest() {
    Integer userId = 1;
    when(timeData.findActiveWorkLog(userId)).thenReturn(Optional.empty());

    assertThrows(
        WorkException.NoActiveWork.class,
        () -> leaveLogService.startLeave(userId)
    );

    verifyNoInteractions(userRepository, leaveLogMapper);
  }

  @Test
  void startLeaveUserOnLeaveExceptionTest() {
    Integer userId = 1;
    WorkLog workLog = new WorkLog();

    when(timeData.findActiveWorkLog(userId)).thenReturn(Optional.of(workLog));
    when(timeData.isUserOnLeave(userId)).thenReturn(true);

    assertThrows(
        WorkException.LeaveStarted.class,
        () -> leaveLogService.startLeave(userId)
    );
  }

  @Test
  void startLeaveUserNotFoundExceptionTest() {
    Integer userId = 1;
    WorkLog workLog = new WorkLog();

    when(timeData.findActiveWorkLog(userId)).thenReturn(Optional.of(workLog));
    when(timeData.isUserOnLeave(userId)).thenReturn(false);
    when(userRepository.findById(userId)).thenReturn(Optional.empty());

    assertThrows(
        IllegalArgumentException.class, () -> leaveLogService.startLeave(userId));
  }

  @Test
  void startLeaveSuccessTest() {
    Integer userId = 1;
    WorkLog workLog = new WorkLog();
    User user = new User();
    LeaveLog savedLeaveLog = new LeaveLog();
    LeaveLogDto expectedDto = new LeaveLogDto();

    when(timeData.findActiveWorkLog(userId)).thenReturn(Optional.of(workLog));
    when(timeData.isUserOnLeave(userId)).thenReturn(false);
    when(userRepository.findById(userId)).thenReturn(Optional.of(user));
    when(timeData.saveLeaveLog(any(LeaveLog.class))).thenReturn(savedLeaveLog);
    when(leaveLogMapper.toDto(savedLeaveLog)).thenReturn(expectedDto);

    LeaveLogDto result = leaveLogService.startLeave(userId);

    assertEquals(expectedDto, result);
    verify(timeData).saveLeaveLog(any(LeaveLog.class));
  }

  @Test
  void endLeaveNoActiveLeaveExceptionTest() {
    Integer userId = 1;
    when(timeData.findActiveLeaveLog(userId)).thenReturn(Optional.empty());

    assertThrows(WorkException.NoActiveLeave.class, () -> leaveLogService.endLeave(userId));
    verifyNoInteractions(leaveLogMapper);
  }

  @Test
  void endLeaveSuccessTest() {
    Integer userId = 1;
    LeaveLog activeLeaveLog = new LeaveLog();
    LeaveLog savedLeaveLog = new LeaveLog();
    LeaveLogDto expectedDto = new LeaveLogDto();

    when(timeData.findActiveLeaveLog(userId)).thenReturn(Optional.of(activeLeaveLog));
    when(timeData.saveLeaveLog(activeLeaveLog)).thenReturn(savedLeaveLog);
    when(leaveLogMapper.toDto(savedLeaveLog)).thenReturn(expectedDto);

    LeaveLogDto result = leaveLogService.endLeave(userId);

    assertEquals(expectedDto, result);
    assertNotNull(activeLeaveLog.getEndTime());
    verify(timeData).saveLeaveLog(activeLeaveLog);
  }

  @Test
  void isUserOnLeave_shouldReturnTrue() {
    when(timeData.isUserOnLeave(1)).thenReturn(true);
    assertTrue(leaveLogService.isUserOnLeave(1));
  }

  @Test
  void isUserOnLeave_shouldReturnFalse() {
    when(timeData.isUserOnLeave(1)).thenReturn(false);
    assertFalse(leaveLogService.isUserOnLeave(1));
  }
}
