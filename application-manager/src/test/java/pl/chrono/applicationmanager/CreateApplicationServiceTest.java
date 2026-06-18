package pl.chrono.applicationmanager;

import java.time.LocalDate;
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

import pl.chrono.applicationmanager.dto.request.CreateApplicationRequest;
import pl.chrono.applicationmanager.dto.response.ApplicationResponse;
import pl.chrono.applicationmanager.event.ApplicationNotificationEvent;
import pl.chrono.applicationmanager.exception.ApplicationUserException;
import pl.chrono.applicationmanager.service.ApplicationService;
import pl.chrono.dbmanager.model.Application;
import pl.chrono.dbmanager.model.User;
import pl.chrono.dbmanager.model.enums.ApplicationStatus;
import pl.chrono.dbmanager.model.enums.ApplicationType;
import pl.chrono.dbmanager.repository.ApplicationRepository;
import pl.chrono.dbmanager.repository.UserRepository;
import pl.chrono.timemanager.service.ApplicationValidationService;
import pl.chrono.timemanager.service.OvertimeService;

@ExtendWith(MockitoExtension.class)
class CreateApplicationServiceTest {

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

  @Test
  void createApplication_ShouldReturnDto_WhenUserExists() {
    Integer employeeId = 1;

    User mockUser = new User();
    mockUser.setUserId(employeeId);
    mockUser.setEmail("pracownik@chrono.pl");

    CreateApplicationRequest requestDto = new CreateApplicationRequest(
        "OVERTIME_EARN",
        LocalDate.of(2026, 5, 1),
        LocalDate.of(2026, 5, 1),
        "Projekt nadgodziny",
        7200
    );

    Application savedApplication = new Application();
    savedApplication.setId(100L);
    savedApplication.setEmployee(mockUser);
    savedApplication.setType(ApplicationType.OVERTIME_EARN);
    savedApplication.setStatus(ApplicationStatus.PENDING);
    savedApplication.setStartDate(requestDto.startDate());
    savedApplication.setEndDate(requestDto.endDate());
    savedApplication.setReason(requestDto.reason());
    savedApplication.setDurationSeconds(7200);
    savedApplication.setCreationDateTime(java.time.LocalDateTime.now());

    when(userRepository.findById(employeeId)).thenReturn(Optional.of(mockUser));
    when(applicationRepository.save(any(Application.class))).thenReturn(savedApplication);

    ApplicationResponse result = applicationService.createApplication(requestDto, employeeId);

    assertNotNull(result);
    assertEquals(100L, result.id());
    assertEquals("OVERTIME_EARN", result.type());
    assertEquals("PENDING", result.status());
    assertEquals(employeeId, result.employeeId());
    assertEquals(7200, result.durationSeconds());
    assertNotNull(result.creationDateTime());

    verify(applicationRepository, times(1)).save(any(Application.class));
    verify(eventPublisher, times(1)).publishEvent(any(ApplicationNotificationEvent.class));
  }

  @Test
  void createApplication_ShouldThrowException_WhenUserDoesNotExist() {
    Integer employeeId = 999;
    CreateApplicationRequest requestDto = new CreateApplicationRequest(
        "EARLY_EXIT", LocalDate.now(), LocalDate.now(), "Test", 3600
    );

    when(userRepository.findById(employeeId)).thenReturn(Optional.empty());

    assertThrows(ApplicationUserException.NotFound.class, () ->
        applicationService.createApplication(requestDto, employeeId)
    );
  }

  @Test
  void createApplication_ShouldReturnDto_WhenEmployeeHasManager() {
    Integer employeeId = 1;

    User mockManager = new User();
    mockManager.setUserId(2);

    User mockUser = new User();
    mockUser.setUserId(employeeId);
    mockUser.setEmail("pracownik@chrono.pl");
    mockUser.setManager(mockManager);

    CreateApplicationRequest requestDto = new CreateApplicationRequest(
        "OVERTIME_EARN",
        LocalDate.of(2026, 5, 1),
        LocalDate.of(2026, 5, 1),
        null,
        null
    );

    Application savedApplication = new Application();
    savedApplication.setId(100L);
    savedApplication.setEmployee(mockUser);
    savedApplication.setType(ApplicationType.OVERTIME_EARN);
    savedApplication.setStatus(ApplicationStatus.PENDING);
    savedApplication.setStartDate(requestDto.startDate());
    savedApplication.setEndDate(requestDto.endDate());
    savedApplication.setCreationDateTime(java.time.LocalDateTime.now());

    when(userRepository.findById(employeeId)).thenReturn(Optional.of(mockUser));
    when(applicationRepository.save(any(Application.class))).thenReturn(savedApplication);

    ApplicationResponse result = applicationService.createApplication(requestDto, employeeId);

    assertNotNull(result);
    verify(eventPublisher, times(1)).publishEvent(any(ApplicationNotificationEvent.class));
  }

  @Test
  void createApplication_ShouldReserveOvertime_WhenTypeIsOvertimeTake() {
    Integer employeeId = 1;
    User mockUser = new User();
    mockUser.setUserId(employeeId);

    CreateApplicationRequest requestDto = new CreateApplicationRequest(
        "OVERTIME_TAKE", LocalDate.now(), LocalDate.now(), "Odpoczynek", 7200
    );

    Application savedApplication = new Application();
    savedApplication.setId(10L);
    savedApplication.setEmployee(mockUser);
    savedApplication.setType(ApplicationType.OVERTIME_TAKE);
    savedApplication.setStatus(ApplicationStatus.PENDING);
    savedApplication.setDurationSeconds(7200);
    savedApplication.setCreationDateTime(java.time.LocalDateTime.now());

    when(userRepository.findById(employeeId)).thenReturn(Optional.of(mockUser));
    when(applicationRepository.save(any(Application.class))).thenReturn(savedApplication);

    applicationService.createApplication(requestDto, employeeId);

    verify(overtimeService, times(1)).reserveOvertimeForApplication(savedApplication);
  }
}
