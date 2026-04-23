package pl.chrono.applicationmanager;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pl.chrono.applicationmanager.dto.ApplicationDTO;
import pl.chrono.applicationmanager.service.ApplicationService;
import pl.chrono.dbmanager.model.Application;
import pl.chrono.dbmanager.model.User;
import pl.chrono.dbmanager.model.enums.ApplicationStatus;
import pl.chrono.dbmanager.model.enums.ApplicationType;
import pl.chrono.dbmanager.repository.ApplicationRepository;
import pl.chrono.dbmanager.repository.UserRepository;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

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
}
