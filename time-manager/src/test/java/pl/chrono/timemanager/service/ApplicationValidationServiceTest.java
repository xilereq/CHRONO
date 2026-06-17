package pl.chrono.timemanager.service;

import java.time.LocalDate;
import java.util.Optional;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import pl.chrono.dbmanager.model.Policy;
import pl.chrono.dbmanager.model.User;
import pl.chrono.dbmanager.model.UsersPolicy;
import pl.chrono.dbmanager.model.enums.ApplicationType;
import pl.chrono.dbmanager.repository.UserRepository;
import pl.chrono.timemanager.exception.ApplicationValidationException;
import pl.chrono.timemanager.exception.UserException;

@ExtendWith(MockitoExtension.class)
class ApplicationValidationServiceTest {

  private final LocalDate startDate = LocalDate.of(2026, 6, 1);
  private final LocalDate endDate = LocalDate.of(2026, 6, 5);
  @Mock
  private UserRepository userRepository;
  @Mock
  private LeaveBalanceService leaveBalanceService;
  @Mock
  private WorkTimeCalculationService workingDaysCalculationService;
  @InjectMocks
  private ApplicationValidationService validationService;
  private User user;
  private Policy policy;

  @BeforeEach
  void setUp() {
    user = new User();
    user.setUserId(1);
    policy = new Policy();
    policy.setDailyHours(8);
    policy.setMaxDailyHours(12);
    UsersPolicy usersPolicy = new UsersPolicy();
    usersPolicy.setPolicy(policy);
    usersPolicy.setUser(user);
    usersPolicy.setValidFrom(LocalDate.now().minusDays(10));
  }

  @Test
  void validateUserNotFoundExceptionTest() {
    when(userRepository.findById(99)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> validationService.validateApplicationLimits(
        99, startDate, endDate,
        ApplicationType.HOLIDAY_LEAVE, 0
    )).isInstanceOf(UserException.NotFound.class);
  }

  @Test
  void validateNullPolicyUsesDefaultLimitTest() {
    when(userRepository.findById(1)).thenReturn(Optional.of(user));

    assertThatThrownBy(() -> validationService.validateApplicationLimits(
        1, startDate, endDate,
        ApplicationType.OVERTIME_EARN, 5 * 3600
    )).isInstanceOf(ApplicationValidationException.DailyLimitExceeded.class);
  }

  @Test
  void validateNullMaxDailyHoursUsesDefaultLimitTest() {
    User spyUser = spy(new User());
    spyUser.setUserId(1);

    Policy spyPolicy = spy(new Policy());
    spyPolicy.setMaxDailyHours(null);

    doReturn(8 * 3600).when(spyPolicy).getRequiredSecondsForDate(startDate);

    doReturn(spyPolicy).when(spyUser).getActivePolicy();

    when(userRepository.findById(1)).thenReturn(Optional.of(spyUser));

    assertThatThrownBy(() -> validationService.validateApplicationLimits(
        1, startDate, endDate,
        ApplicationType.OVERTIME_EARN, 5 * 3600
    )).isInstanceOf(ApplicationValidationException.DailyLimitExceeded.class);
  }

  @Test
  void validateHolidayLeaveInsufficientBalanceExceptionTest() {
    User spyUser = spy(new User());
    spyUser.setUserId(1);
    doReturn(policy).when(spyUser).getActivePolicy();
    when(userRepository.findById(1)).thenReturn(Optional.of(spyUser));

    when(workingDaysCalculationService.calculateWorkingDays(startDate, endDate, policy)).thenReturn(
        10);

    when(leaveBalanceService.calculateAnnualLeaveLimit(spyUser)).thenReturn(5);
    when(leaveBalanceService.getRolloverDaysFromPreviousYear(1, startDate.getYear())).thenReturn(2);
    when(leaveBalanceService.getUsedLeaveDaysInYear(1, startDate.getYear())).thenReturn(1);

    assertThatThrownBy(() -> validationService.validateApplicationLimits(
        1, startDate, endDate,
        ApplicationType.HOLIDAY_LEAVE, null
    )).isInstanceOf(ApplicationValidationException.InsufficientLeaveBalance.class);
  }

  @Test
  void validateHolidayPassTest() {
    User spyUser = spy(new User());
    spyUser.setUserId(1);
    doReturn(policy).when(spyUser).getActivePolicy();
    when(userRepository.findById(1)).thenReturn(Optional.of(spyUser));

    when(workingDaysCalculationService.calculateWorkingDays(startDate, endDate, policy)).thenReturn(
        5);

    when(leaveBalanceService.calculateAnnualLeaveLimit(spyUser)).thenReturn(20);
    when(leaveBalanceService.getRolloverDaysFromPreviousYear(1, startDate.getYear())).thenReturn(0);
    when(leaveBalanceService.getUsedLeaveDaysInYear(1, startDate.getYear())).thenReturn(5);

    assertThatCode(() -> validationService.validateApplicationLimits(
        1, startDate, endDate,
        ApplicationType.HOLIDAY_LEAVE, null
    )).doesNotThrowAnyException();
  }

  @Test
  void holidayLeaveNullDurationReturnTest() {
    User spyUser = spy(new User());
    spyUser.setUserId(1);
    doReturn(policy).when(spyUser).getActivePolicy();
    when(userRepository.findById(1)).thenReturn(Optional.of(spyUser));

    when(workingDaysCalculationService.calculateWorkingDays(startDate, endDate, policy)).thenReturn(
        -1);

    assertThatCode(() -> validationService.validateApplicationLimits(
        1, startDate, endDate,
        ApplicationType.HOLIDAY_LEAVE, null
    )).doesNotThrowAnyException();

    verify(leaveBalanceService, never()).calculateAnnualLeaveLimit(any());
  }

  @Test
  void overtimeEarnNullDurationReturnTest() {
    when(userRepository.findById(1)).thenReturn(Optional.of(user));

    assertThatCode(() -> validationService.validateApplicationLimits(
        1, startDate, endDate,
        ApplicationType.OVERTIME_EARN, null
    )).doesNotThrowAnyException();
  }

  @Test
  void OvertimeEarnZeroDurationReturnTest() {
    when(userRepository.findById(1)).thenReturn(Optional.of(user));

    assertThatCode(() -> validationService.validateApplicationLimits(
        1, startDate, endDate,
        ApplicationType.OVERTIME_EARN, 0
    )).doesNotThrowAnyException();
  }

  @Test
  void overtimeEarnPassTest() {
    when(userRepository.findById(1)).thenReturn(Optional.of(user));

    assertThatCode(() -> validationService.validateApplicationLimits(
        1, startDate, endDate,
        ApplicationType.OVERTIME_EARN, 2 * 3600
    )).doesNotThrowAnyException();
  }


  @Test
  void earlyExitNullValueReturnTest() {
    when(userRepository.findById(1)).thenReturn(Optional.of(user));

    assertThatCode(() -> validationService.validateApplicationLimits(
        1, startDate, endDate,
        ApplicationType.EARLY_EXIT, null
    )).doesNotThrowAnyException();
  }

  @Test
  void earlyExitNegativeValueReturnTest() {
    when(userRepository.findById(1)).thenReturn(Optional.of(user));

    assertThatCode(() -> validationService.validateApplicationLimits(
        1, startDate, endDate,
        ApplicationType.EARLY_EXIT, -3600
    )).doesNotThrowAnyException();
  }

  @Test
  void validateEarlyExit_ExceedsBaseTime_ThrowsException() {
    when(userRepository.findById(1)).thenReturn(Optional.of(user));

    assertThatThrownBy(() -> validationService.validateApplicationLimits(
        1, startDate, endDate,
        ApplicationType.EARLY_EXIT, 9 * 3600
    )).isInstanceOf(ApplicationValidationException.RequestedTimeExceedsScheduled.class);
  }

  @Test
  void validateOvertimeTakePassTest() {
    when(userRepository.findById(1)).thenReturn(Optional.of(user));

    assertThatCode(() -> validationService.validateApplicationLimits(
        1, startDate, endDate,
        ApplicationType.OVERTIME_TAKE, 4 * 3600
    )).doesNotThrowAnyException();
  }


  @Test
  void validateLeavePassPassTest() {
    when(userRepository.findById(1)).thenReturn(Optional.of(user));

    assertThatCode(() -> validationService.validateApplicationLimits(
        1, startDate, endDate,
        ApplicationType.SICK_LEAVE, 8 * 3600
    )).doesNotThrowAnyException();
  }
}
