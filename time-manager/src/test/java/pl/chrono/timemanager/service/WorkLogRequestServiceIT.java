package pl.chrono.timemanager.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import pl.chrono.dbmanager.model.LeaveLog;
import pl.chrono.dbmanager.model.OvertimeArchive;
import pl.chrono.dbmanager.model.Policy;
import pl.chrono.dbmanager.model.Role;
import pl.chrono.dbmanager.model.User;
import pl.chrono.dbmanager.model.UsersPolicy;
import pl.chrono.dbmanager.model.WorkLog;
import pl.chrono.dbmanager.model.WorkLogRequest;
import pl.chrono.dbmanager.model.enums.OvertimeType;
import pl.chrono.dbmanager.model.enums.WorkLogRequestStatus;
import pl.chrono.dbmanager.repository.OvertimeArchiveRepository;
import pl.chrono.dbmanager.repository.PolicyRepository;
import pl.chrono.dbmanager.repository.RoleRepository;
import pl.chrono.dbmanager.repository.UserRepository;
import pl.chrono.dbmanager.repository.UsersPolicyRepository;
import pl.chrono.dbmanager.repository.WorkLogRepository;
import pl.chrono.dbmanager.repository.WorkLogRequestRepository;
import pl.chrono.timemanager.TestConfig;
import pl.chrono.timemanager.dto.request.WorkLogCreateRequest;
import pl.chrono.timemanager.dto.request.WorkLogResolveRequest;
import pl.chrono.timemanager.dto.response.WorkLogRequestDto;
import pl.chrono.timemanager.exception.UserException;
import pl.chrono.timemanager.exception.WorkLogRequestException;

@SpringBootTest(classes = TestConfig.class)
@Transactional
class WorkLogRequestServiceIT {

  @Autowired
  private WorkLogRequestService workLogRequestService;

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private RoleRepository roleRepository;

  @Autowired
  private PolicyRepository policyRepository;

  @Autowired
  private UsersPolicyRepository usersPolicyRepository;

  @Autowired
  private WorkLogRepository workLogRepository;

  @Autowired
  private WorkLogRequestRepository workLogRequestRepository;

  @Autowired
  private OvertimeArchiveRepository overtimeArchiveRepository;

  @PersistenceContext
  private EntityManager entityManager;

  private User manager;
  private User employee;

  @BeforeEach
  void setUp() {
    manager = userRepository.save(buildUser("Marek", "Kierownik", "marek.kierownik@test.com"));
    employee = userRepository.save(buildUser("Jan", "Kowalski", "jan.kowalski@test.com"));
    employee.setManager(manager);
    userRepository.save(employee);
    assignPolicy(employee, 8, 10);
  }

  @Test
  void submitRequest_createsPendingRequest_whenWithinPolicyBounds() {
    workLogRequestService.submitRequest(employee.getUserId(), createRequest(9, "Worked late"));

    WorkLogRequest saved = findSinglePendingRequest(employee.getUserId());
    assertThat(saved.getStatus()).isEqualTo(WorkLogRequestStatus.PENDING);
    assertThat(saved.getReason()).isEqualTo("Worked late");
  }

  @Test
  void submitRequest_throwsInvalidTimeRange_whenStartIsAfterEnd() {
    LocalDateTime start = LocalDate.now().plusDays(1).atTime(17, 0);
    LocalDateTime end = LocalDate.now().plusDays(1).atTime(8, 0);
    WorkLogCreateRequest request = new WorkLogCreateRequest(
        start.toLocalDate(), start, end,
        "Bad range"
    );

    assertThatThrownBy(
        () -> workLogRequestService.submitRequest(employee.getUserId(), request)).isInstanceOf(
        WorkLogRequestException.InvalidTimeRange.class);
  }

  @Test
  void submitRequest_throwsUserNotFound_whenEmployeeDoesNotExist() {
    assertThatThrownBy(
        () -> workLogRequestService.submitRequest(999999, createRequest(9, "n/a"))).isInstanceOf(
        UserException.NotFound.class);
  }

  @Test
  void submitRequest_throwsDailyLimitExceeded_whenRequestedTimeExceedsPolicyMax() {
    assertThatThrownBy(() -> workLogRequestService.submitRequest(
        employee.getUserId(),
        createRequest(11, "Too long")
    )).isInstanceOf(WorkLogRequestException.DailyLimitExceeded.class);
  }

  @Test
  void submitRequest_throwsInsufficientTime_whenRequestedTimeBelowRequired() {
    assertThatThrownBy(() -> workLogRequestService.submitRequest(
        employee.getUserId(),
        createRequest(2, "Too short")
    )).isInstanceOf(WorkLogRequestException.InsufficientTime.class);
  }

  @Test
  void approveRequest_createsNewWorkLog_whenNoneExistsForThatDay() {
    workLogRequestService.submitRequest(employee.getUserId(), createRequest(9, "Worked late"));
    WorkLogRequest request = findSinglePendingRequest(employee.getUserId());

    workLogRequestService.approveRequest(
        request.getId(), manager.getUserId(),
        new WorkLogResolveRequest("Looks good")
    );

    WorkLogRequest updated = workLogRequestRepository.findById(request.getId()).orElseThrow();
    assertThat(updated.getStatus()).isEqualTo(WorkLogRequestStatus.APPROVED);
    assertThat(updated.getManager().getUserId()).isEqualTo(manager.getUserId());

    List<WorkLog> logsForDay =
        workLogRepository.findByUser_UserIdAndPendingCorrectionFalseAndStartTimeBetweenOrderByStartTimeAsc(
            employee.getUserId(), request.getProposedStartTime().toLocalDate().atStartOfDay(),
            request.getProposedStartTime().toLocalDate().plusDays(1).atStartOfDay()
        );
    assertThat(logsForDay).hasSize(1);
  }

  @Test
  void approveRequest_updatesExistingWorkLog_andClearsLeaveLogs() {
    workLogRequestService.submitRequest(employee.getUserId(), createRequest(9, "Correction"));
    WorkLogRequest request = findSinglePendingRequest(employee.getUserId());
    LocalDate targetDate = request.getProposedStartTime().toLocalDate();

    WorkLog existing = persistWorkLog(targetDate.atTime(7, 0), targetDate.atTime(15, 0));

    persistLeaveLog(existing, targetDate.atTime(10, 0), null);

    workLogRequestService.approveRequest(request.getId(), manager.getUserId(), null);

    entityManager.flush();
    entityManager.clear();

    WorkLog reloaded = workLogRepository.findById(existing.getWorkLogId()).orElseThrow();
    assertThat(reloaded.getStartTime()).isEqualTo(request.getProposedStartTime());
    assertThat(reloaded.getEndTime()).isEqualTo(request.getProposedEndTime());

    assertThat(reloaded.getLeaveLogs()).isEmpty();

    List<WorkLog> logsForDay =
        workLogRepository.findByUser_UserIdAndPendingCorrectionFalseAndStartTimeBetweenOrderByStartTimeAsc(
            employee.getUserId(), targetDate.atStartOfDay(), targetDate.plusDays(1).atStartOfDay());
    assertThat(logsForDay).hasSize(1);
  }

  @Test
  void approveRequest_revertsSettlement_whenExistingWorkLogWasAlreadySettled() {
    workLogRequestService.submitRequest(employee.getUserId(), createRequest(9, "Correction"));
    WorkLogRequest request = findSinglePendingRequest(employee.getUserId());
    LocalDate targetDate = request.getProposedStartTime().toLocalDate();

    WorkLog existing = persistWorkLog(targetDate.atTime(7, 0), targetDate.atTime(15, 0));
    existing.setSettled(true);
    workLogRepository.save(existing);
    persistEarnedArchive(existing, 3600);

    workLogRequestService.approveRequest(request.getId(), manager.getUserId(), null);

    entityManager.flush();
    entityManager.clear();

    WorkLog reloaded = workLogRepository.findById(existing.getWorkLogId()).orElseThrow();
    assertThat(reloaded.isSettled()).isFalse();
    assertThat(
        overtimeArchiveRepository.findByWorkLog_WorkLogId(existing.getWorkLogId())).isEmpty();
  }

  @Test
  void approveRequest_throwsNotFound_whenRequestDoesNotExist() {
    assertThatThrownBy(() -> workLogRequestService.approveRequest(
        999999L, manager.getUserId(),
        null
    )).isInstanceOf(WorkLogRequestException.NotFound.class);
  }

  @Test
  void approveRequest_throwsInvalidState_whenRequestAlreadyResolved() {
    workLogRequestService.submitRequest(employee.getUserId(), createRequest(9, "Worked late"));
    WorkLogRequest request = findSinglePendingRequest(employee.getUserId());
    workLogRequestService.approveRequest(request.getId(), manager.getUserId(), null);

    assertThatThrownBy(
        () -> workLogRequestService.approveRequest(
            request.getId(), manager.getUserId(),
            null
        )).isInstanceOf(WorkLogRequestException.InvalidState.class);
  }

  @Test
  void approveRequest_throwsUnauthorizedManager_whenReviewerIsNotDirectManagerOrAdmin() {
    User otherManager = userRepository.save(buildUser("Ola", "Inna", "ola.inna@test.com"));
    workLogRequestService.submitRequest(employee.getUserId(), createRequest(9, "Worked late"));
    WorkLogRequest request = findSinglePendingRequest(employee.getUserId());

    assertThatThrownBy(
        () -> workLogRequestService.approveRequest(
            request.getId(), otherManager.getUserId(), null)).isInstanceOf(
        WorkLogRequestException.UnauthorizedManager.class);
  }

  @Test
  void approveRequest_succeedsForAdmin_evenWhenNotDirectManager() {
    User admin = userRepository.save(buildUser("Adam", "Admin", "adam.admin@test.com"));
    Role adminRole = roleRepository.save(buildRole("ROLE_ADMIN"));
    admin.getRoles().add(adminRole);
    userRepository.save(admin);

    workLogRequestService.submitRequest(employee.getUserId(), createRequest(9, "Worked late"));
    WorkLogRequest request = findSinglePendingRequest(employee.getUserId());

    workLogRequestService.approveRequest(request.getId(), admin.getUserId(), null);

    WorkLogRequest updated = workLogRequestRepository.findById(request.getId()).orElseThrow();
    assertThat(updated.getStatus()).isEqualTo(WorkLogRequestStatus.APPROVED);
  }

  @Test
  void rejectRequest_setsStatusRejectedWithComment() {
    workLogRequestService.submitRequest(employee.getUserId(), createRequest(9, "Worked late"));
    WorkLogRequest request = findSinglePendingRequest(employee.getUserId());

    workLogRequestService.rejectRequest(
        request.getId(), manager.getUserId(),
        new WorkLogResolveRequest("Not enough evidence")
    );

    WorkLogRequest updated = workLogRequestRepository.findById(request.getId()).orElseThrow();
    assertThat(updated.getStatus()).isEqualTo(WorkLogRequestStatus.REJECTED);
    assertThat(updated.getManagerComment()).isEqualTo("Not enough evidence");
  }

  @Test
  void rejectRequest_throwsUnauthorizedManager_whenReviewerIsNotDirectManagerOrAdmin() {
    User otherManager = userRepository.save(buildUser("Ola", "Inna", "ola.inna2@test.com"));
    workLogRequestService.submitRequest(employee.getUserId(), createRequest(9, "Worked late"));
    WorkLogRequest request = findSinglePendingRequest(employee.getUserId());

    assertThatThrownBy(
        () -> workLogRequestService.rejectRequest(
            request.getId(), otherManager.getUserId(), null)).isInstanceOf(
        WorkLogRequestException.UnauthorizedManager.class);
  }

  @Test
  void getEmployeeRequests_returnsAllRequestsForThatEmployee() {
    workLogRequestService.submitRequest(employee.getUserId(), createRequest(9, "First"));
    workLogRequestService.submitRequest(employee.getUserId(), createRequest(9, "Second"));

    List<WorkLogRequestDto> result = workLogRequestService.getEmployeeRequests(
        employee.getUserId());

    assertThat(result).hasSize(2);
    assertThat(result).allMatch(dto -> dto.employeeId().equals(employee.getUserId()));
  }

  @Test
  void getPendingRequestsForReviewer_managerSeesOnlyOwnSubordinatesRequests() {
    User otherManager = userRepository.save(buildUser("Ola", "Inna", "ola.inna3@test.com"));
    User otherEmployee = userRepository.save(buildUser("Ewa", "Druga", "ewa.druga@test.com"));
    otherEmployee.setManager(otherManager);
    userRepository.save(otherEmployee);
    assignPolicy(otherEmployee, 8, 10);

    workLogRequestService.submitRequest(employee.getUserId(), createRequest(9, "Mine"));
    workLogRequestService.submitRequest(otherEmployee.getUserId(), createRequest(9, "Not mine"));

    List<WorkLogRequestDto> result = workLogRequestService.getPendingRequestsForReviewer(
        manager.getUserId());

    assertThat(result).hasSize(1);
    assertThat(result.get(0).employeeId()).isEqualTo(employee.getUserId());
  }

  @Test
  void getPendingRequestsForReviewer_adminSeesAllPendingRequests() {
    User admin = userRepository.save(buildUser("Adam", "Admin", "adam.admin2@test.com"));
    Role adminRole = roleRepository.save(buildRole("ROLE_ADMIN"));
    admin.getRoles().add(adminRole);
    userRepository.save(admin);

    User otherManager = userRepository.save(buildUser("Ola", "Inna", "ola.inna4@test.com"));
    User otherEmployee = userRepository.save(buildUser("Ewa", "Druga", "ewa.druga2@test.com"));
    otherEmployee.setManager(otherManager);
    userRepository.save(otherEmployee);
    assignPolicy(otherEmployee, 8, 10);

    workLogRequestService.submitRequest(employee.getUserId(), createRequest(9, "Mine"));
    workLogRequestService.submitRequest(otherEmployee.getUserId(), createRequest(9, "Not mine"));

    List<WorkLogRequestDto> result = workLogRequestService.getPendingRequestsForReviewer(
        admin.getUserId());

    assertThat(result).hasSize(2);
    assertThat(result).extracting(WorkLogRequestDto::employeeId)
        .containsExactlyInAnyOrder(employee.getUserId(), otherEmployee.getUserId());
  }

  private User buildUser(String name, String surname, String email) {
    User user = new User();
    user.setName(name);
    user.setSurname(surname);
    user.setEmail(email);
    user.setPasswordHash("hash");
    return user;
  }

  private Role buildRole(String name) {
    Role role = new Role();
    role.setName(name);
    return role;
  }

  private void assignPolicy(User employeeUser, int dailyHours, int maxDailyHours) {
    Policy policy = new Policy();
    policy.setName("Standard - all days");
    policy.setDailyHours(dailyHours);
    policy.setMaxDailyHours(maxDailyHours);
    policy.setMaxWeeklyHours(48);
    policy.setRoundingMinutes(0);
    policy.setWorkingDays(new HashSet<>(Arrays.asList(DayOfWeek.values())));
    policyRepository.save(policy);

    UsersPolicy usersPolicy = new UsersPolicy();
    usersPolicy.setUser(employeeUser);
    usersPolicy.setPolicy(policy);
    usersPolicy.setValidFrom(LocalDate.now().minusYears(1));
    usersPolicy.setValidTo(null);
    usersPolicyRepository.save(usersPolicy);

    entityManager.flush();
    entityManager.clear();
  }

  private WorkLogCreateRequest createRequest(int durationHours, String reason) {
    LocalDateTime start = LocalDate.now().plusDays(1).atTime(8, 0);
    LocalDateTime end = start.plusHours(durationHours);
    return new WorkLogCreateRequest(start.toLocalDate(), start, end, reason);
  }

  private WorkLogRequest findSinglePendingRequest(Integer employeeId) {
    return workLogRequestRepository.findAllByEmployeeUserIdOrderByCreatedAtDesc(employeeId).stream()
        .filter(r -> r.getStatus() == WorkLogRequestStatus.PENDING).findFirst().orElseThrow();
  }

  private WorkLog persistWorkLog(LocalDateTime startTime, LocalDateTime endTime) {
    WorkLog workLog = new WorkLog();
    workLog.setUser(employee);
    workLog.setStartTime(startTime);
    workLog.setEndTime(endTime);
    return workLogRepository.save(workLog);
  }

  private void persistLeaveLog(WorkLog workLog, LocalDateTime start, LocalDateTime end) {
    LeaveLog leaveLog = new LeaveLog();
    leaveLog.setWorkLog(workLog);
    leaveLog.setStartTime(start);
    leaveLog.setEndTime(end);
    leaveLog.setUser(workLog.getUser());
    workLog.getLeaveLogs().add(leaveLog);
    workLogRepository.save(workLog);
  }

  private void persistEarnedArchive(WorkLog workLog, int durationSeconds) {
    OvertimeArchive archive = new OvertimeArchive();
    archive.setUser(employee);
    archive.setType(OvertimeType.EARNED);
    archive.setDurationSeconds(durationSeconds);
    archive.setWorkLog(workLog);
    archive.setValidUntil(LocalDateTime.now().plusMonths(3));
    overtimeArchiveRepository.save(archive);
  }
}
