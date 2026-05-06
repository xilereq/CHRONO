package pl.chrono.applicationmanager;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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

    when(userRepository.findById(employeeId)).thenReturn(Optional.of(mockUser));
    when(applicationRepository.save(any(Application.class))).thenReturn(savedApplication);

    ApplicationDTO result = applicationService.createApplication(requestDto, employeeId);

    assertNotNull(result);
    assertEquals(100L, result.getId());
    assertEquals("HOLIDAY_LEAVE", result.getType());
    assertEquals("PENDING", result.getStatus());
    assertEquals(employeeId, result.getEmployeeId());

    verify(applicationRepository, times(1)).save(any(Application.class));
  }

  @Test
  void changeStatus_ShouldUpdateStatusAndReviewer_WhenBothExist() {
    Long applicationId = 1L;
    Integer reviewerId = 2;
    String newStatus = "APPROVED";
    String decisionReason = "Valid reason";

    Application mockApplication = new Application();
    mockApplication.setId(applicationId);
    mockApplication.setStatus(ApplicationStatus.PENDING);

    User mockEmployee = new User();
    mockEmployee.setUserId(1);
    mockApplication.setEmployee(mockEmployee);
    mockApplication.setType(ApplicationType.HOLIDAY_LEAVE);

    User mockReviewer = new User();
    mockReviewer.setUserId(reviewerId);

    when(applicationRepository.findById(applicationId)).thenReturn(Optional.of(mockApplication));
    when(userRepository.findById(reviewerId)).thenReturn(Optional.of(mockReviewer));
    when(applicationRepository.save(any(Application.class))).thenReturn(mockApplication);

    ApplicationDTO result = applicationService.changeStatus(applicationId, newStatus, reviewerId, decisionReason);

    assertNotNull(result);
    assertEquals(newStatus, result.getStatus());
    assertEquals(decisionReason, result.getDecisionReason());
    verify(applicationRepository, times(1)).save(mockApplication);
  }

  @Test
  void getUserApplications_ShouldReturnListOfDtos() {
    Integer employeeId = 1;

    User mockEmployee = new User();
    mockEmployee.setUserId(employeeId);

    Application mockApplication = new Application();
    mockApplication.setId(10L);
    mockApplication.setEmployee(mockEmployee);
    mockApplication.setType(ApplicationType.SICK_LEAVE);
    mockApplication.setStatus(ApplicationStatus.PENDING);

    when(applicationRepository.findAllByEmployeeUserId(employeeId))
        .thenReturn(List.of(mockApplication));

    List<ApplicationDTO> result = applicationService.getUserApplications(employeeId);

    assertNotNull(result);
    assertEquals(1, result.size());
    assertEquals(10L, result.getFirst().getId());
    assertEquals("SICK_LEAVE", result.getFirst().getType());
  }
}
