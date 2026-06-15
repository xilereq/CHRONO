package pl.chrono.timemanager.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pl.chrono.dbmanager.model.Application;
import pl.chrono.dbmanager.model.Policy;
import pl.chrono.dbmanager.model.User;
import pl.chrono.dbmanager.model.enums.ApplicationStatus;
import pl.chrono.dbmanager.model.enums.ApplicationType;
import pl.chrono.dbmanager.repository.ApplicationRepository;
import pl.chrono.dbmanager.repository.UserRepository;
import pl.chrono.timemanager.exception.UserException;

@ExtendWith(MockitoExtension.class)
class LeaveBalanceServiceTest {

  @Mock
  private ApplicationRepository applicationRepository;

  @Mock
  private WorkTimeCalculationService workingDaysCalculationService;

  @Mock
  private UserRepository userRepository;

  @InjectMocks
  private LeaveBalanceService leaveBalanceService;

  private final Integer USER_ID = 1;

  @Test
  void calculateAnnualLeaveLimitBaseLimitTest() {
    User user = new User();
    user.setEmploymentStartDate(null);

    int limit = leaveBalanceService.calculateAnnualLeaveLimit(user, 2026);

    assertThat(limit).isEqualTo(26);
  }

  @Test
  void calculateAnnualLeaveLimitYearsOfServiceTest() {
    User user = new User();
    user.setEmploymentStartDate(LocalDate.of(2023, 1, 1));

    int limit = leaveBalanceService.calculateAnnualLeaveLimit(user, 2026);

    assertThat(limit).isEqualTo(29);
  }

  @Test
  void calculateAnnualLeaveLimitedTest() {
    User user = new User();
    user.setEmploymentStartDate(LocalDate.of(2016, 1, 1));

    int limit = leaveBalanceService.calculateAnnualLeaveLimit(user, 2026);

    assertThat(limit).isEqualTo(31);
  }

  @Test
  void calculateAnnualLeaveLimitCalculateTest() {
    User user = new User();
    user.setEmploymentStartDate(LocalDate.now().minusYears(2));

    int limit = leaveBalanceService.calculateAnnualLeaveLimit(user);

    assertThat(limit).isEqualTo(28);
  }

  @Test
  void getUsedLeaveDaysInYearUserNotFoundExceptionTest() {
    when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

    assertThatThrownBy(
        () -> leaveBalanceService.getUsedLeaveDaysInYear(USER_ID, 2026)).isInstanceOf(
        UserException.NotFound.class);
  }

  @Test
  void getUsedLeaveDaysInYearCalculatesProperlyTest() {
    User spyUser = spy(new User());
    spyUser.setUserId(USER_ID);
    Policy policy = new Policy();
    doReturn(policy).when(spyUser).getPolicyForDate(any());

    when(userRepository.findById(USER_ID)).thenReturn(Optional.of(spyUser));

    Application app1 = new Application();
    app1.setStartDate(LocalDate.of(2026, 6, 1));
    app1.setEndDate(LocalDate.of(2026, 6, 5));

    Application app2 = new Application();
    app2.setStartDate(LocalDate.of(2026, 7, 1));
    app2.setEndDate(LocalDate.of(2026, 7, 2));

    when(applicationRepository.findApplicationsByEmployeeAndTypeAndDateRange(
        eq(USER_ID),
        eq(ApplicationType.HOLIDAY_LEAVE), eq(ApplicationStatus.APPROVED), any(), any()
    )).thenReturn(List.of(app1, app2));

    when(workingDaysCalculationService.calculateWorkingDays(
        app1.getStartDate(), app1.getEndDate(),
        policy
    )).thenReturn(5);
    when(workingDaysCalculationService.calculateWorkingDays(
        app2.getStartDate(), app2.getEndDate(),
        policy
    )).thenReturn(2);

    int usedDays = leaveBalanceService.getUsedLeaveDaysInYear(USER_ID, 2026);

    assertThat(usedDays).isEqualTo(7);
  }

  @Test
  void getRolloverDaysFromPreviousYearUserNotFoundTest() {
    when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

    assertThatThrownBy(
        () -> leaveBalanceService.getRolloverDaysFromPreviousYear(USER_ID, 2026)).isInstanceOf(
        UserException.NotFound.class);
  }

  @Test
  void getRolloverDaysFromPreviousYearNegativeValueTest() {
    User user = new User();
    user.setEmploymentStartDate(null);
    when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));

    Application app = new Application();
    app.setStartDate(LocalDate.of(2025, 1, 1));
    app.setEndDate(LocalDate.of(2025, 1, 1));
    when(applicationRepository.findApplicationsByEmployeeAndTypeAndDateRange(
        eq(USER_ID),
        eq(ApplicationType.HOLIDAY_LEAVE), eq(ApplicationStatus.APPROVED), any(), any()
    )).thenReturn(List.of(app));

    when(workingDaysCalculationService.calculateWorkingDays(any(), any(), any())).thenReturn(30);

    int rollover = leaveBalanceService.getRolloverDaysFromPreviousYear(USER_ID, 2026);

    assertThat(rollover).isZero();
  }
}
