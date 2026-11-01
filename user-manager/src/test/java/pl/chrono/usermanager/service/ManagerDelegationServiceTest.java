package pl.chrono.usermanager.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import pl.chrono.dbmanager.model.ManagerDelegation;
import pl.chrono.dbmanager.model.Role;
import pl.chrono.dbmanager.model.User;
import pl.chrono.dbmanager.ports.I_ManagerDelegationData;
import pl.chrono.dbmanager.ports.I_UserData;
import pl.chrono.usermanager.dto.request.CreateManagerDelegationRequest;
import pl.chrono.usermanager.dto.response.ManagerDelegationDto;
import pl.chrono.usermanager.event.ManagerDelegationEvent;
import pl.chrono.usermanager.exception.ManagerDelegationException;

@ExtendWith(MockitoExtension.class)
class ManagerDelegationServiceTest {

  private static final Integer MANAGER_ID = 1;
  private static final Integer DEPUTY_ID = 2;
  private static final Integer OTHER_MANAGER_ID = 3;
  private static final Integer HR_ID = 4;

  private static final LocalDate TODAY = LocalDate.now();
  private static final LocalDate START = TODAY.plusDays(1);
  private static final LocalDate END = TODAY.plusDays(10);

  @Mock
  private I_ManagerDelegationData delegationData;
  @Mock
  private I_UserData userData;
  @Mock
  private ApplicationEventPublisher eventPublisher;

  @InjectMocks
  private ManagerDelegationService service;

  private static User user(Integer id, String... roles) {
    User user = new User();
    user.setUserId(id);
    user.setName("Name" + id);
    user.setSurname("Surname" + id);
    for (String roleName : roles) {
      Role role = new Role();
      role.setName(roleName);
      user.getRoles().add(role);
    }
    return user;
  }

  private static ManagerDelegation delegation(User manager, User deputy, LocalDate start, LocalDate end) {
    ManagerDelegation d = new ManagerDelegation();
    d.setId(100L);
    d.setManager(manager);
    d.setDeputy(deputy);
    d.setStartDate(start);
    d.setEndDate(end);
    d.setCreatedBy(manager);
    d.setCreatedAt(LocalDateTime.now());
    return d;
  }

  private void stubUsers(User... users) {
    for (User u : users) {
      when(userData.findById(u.getUserId())).thenReturn(Optional.of(u));
    }
  }

  private void stubSaveReturnsWithId() {
    when(delegationData.save(any(ManagerDelegation.class))).thenAnswer(inv -> {
      ManagerDelegation d = inv.getArgument(0);
      d.setId(100L);
      return d;
    });
  }

  @Test
  void createDelegation_ManagerDelegatesOwnPermissions() {
    User manager = user(MANAGER_ID, "ROLE_MANAGER");
    User deputy = user(DEPUTY_ID, "ROLE_EMPLOYEE");
    stubUsers(manager, deputy);
    stubSaveReturnsWithId();

    ManagerDelegationDto dto = service.createDelegation(
        new CreateManagerDelegationRequest(null, DEPUTY_ID, START, END, "Urlop"), MANAGER_ID);

    assertEquals(MANAGER_ID, dto.managerId());
    assertEquals(DEPUTY_ID, dto.deputyId());
    assertEquals("SCHEDULED", dto.status());
    assertEquals(MANAGER_ID, dto.createdById());

    ArgumentCaptor<ManagerDelegationEvent> captor = ArgumentCaptor.forClass(ManagerDelegationEvent.class);
    verify(eventPublisher).publishEvent(captor.capture());
    assertEquals("CREATED", captor.getValue().action());
    assertEquals(DEPUTY_ID, captor.getValue().deputyId());
  }

  @Test
  void createDelegation_HrCanDelegateOnBehalfOfManager() {
    User hr = user(HR_ID, "ROLE_HR");
    User manager = user(MANAGER_ID, "ROLE_MANAGER");
    User deputy = user(DEPUTY_ID, "ROLE_EMPLOYEE");
    stubUsers(hr, manager, deputy);
    stubSaveReturnsWithId();

    ManagerDelegationDto dto = service.createDelegation(
        new CreateManagerDelegationRequest(MANAGER_ID, DEPUTY_ID, START, END, null), HR_ID);

    assertEquals(MANAGER_ID, dto.managerId());
    assertEquals(HR_ID, dto.createdById());
  }

  @Test
  void createDelegation_ManagerCannotDelegateForAnotherManager() {
    stubUsers(user(MANAGER_ID, "ROLE_MANAGER"));

    assertThrows(ManagerDelegationException.Forbidden.class, () -> service.createDelegation(
        new CreateManagerDelegationRequest(OTHER_MANAGER_ID, DEPUTY_ID, START, END, null), MANAGER_ID));
    verify(delegationData, never()).save(any());
  }

  @Test
  void createDelegation_RequiresRealManagerRole() {
    stubUsers(user(DEPUTY_ID, "ROLE_EMPLOYEE"));

    assertThrows(ManagerDelegationException.NotAManager.class, () -> service.createDelegation(
        new CreateManagerDelegationRequest(null, 5, START, END, null), DEPUTY_ID));
  }

  @Test
  void createDelegation_DeputyCannotBeTheManager() {
    stubUsers(user(MANAGER_ID, "ROLE_MANAGER"));

    assertThrows(ManagerDelegationException.InvalidDeputy.class, () -> service.createDelegation(
        new CreateManagerDelegationRequest(null, MANAGER_ID, START, END, null), MANAGER_ID));
  }

  @Test
  void createDelegation_DeputyMustBeEmployedForWholePeriod() {
    User deputy = user(DEPUTY_ID, "ROLE_EMPLOYEE");
    deputy.setEmploymentEndDate(START.plusDays(2));
    stubUsers(user(MANAGER_ID, "ROLE_MANAGER"), deputy);

    assertThrows(ManagerDelegationException.InvalidDeputy.class, () -> service.createDelegation(
        new CreateManagerDelegationRequest(null, DEPUTY_ID, START, END, null), MANAGER_ID));
  }

  @Test
  void createDelegation_RejectsInactiveDeputy() {
    User deputy = user(DEPUTY_ID, "ROLE_EMPLOYEE");
    deputy.setActive(false);
    stubUsers(user(MANAGER_ID, "ROLE_MANAGER"), deputy);

    assertThrows(ManagerDelegationException.InvalidDeputy.class, () -> service.createDelegation(
        new CreateManagerDelegationRequest(null, DEPUTY_ID, START, END, null), MANAGER_ID));
  }

  @Test
  void createDelegation_RejectsEndBeforeStart() {
    stubUsers(user(MANAGER_ID, "ROLE_MANAGER"));

    assertThrows(ManagerDelegationException.InvalidPeriod.class, () -> service.createDelegation(
        new CreateManagerDelegationRequest(null, DEPUTY_ID, END, START, null), MANAGER_ID));
  }

  @Test
  void createDelegation_RejectsStartInThePast() {
    stubUsers(user(MANAGER_ID, "ROLE_MANAGER"));

    assertThrows(ManagerDelegationException.InvalidPeriod.class, () -> service.createDelegation(
        new CreateManagerDelegationRequest(null, DEPUTY_ID, TODAY.minusDays(1), END, null), MANAGER_ID));
  }

  @Test
  void createDelegation_AllowsStartToday() {
    stubUsers(user(MANAGER_ID, "ROLE_MANAGER"), user(DEPUTY_ID, "ROLE_EMPLOYEE"));
    stubSaveReturnsWithId();

    ManagerDelegationDto dto = service.createDelegation(
        new CreateManagerDelegationRequest(null, DEPUTY_ID, TODAY, END, null), MANAGER_ID);

    assertEquals("ACTIVE", dto.status());
  }

  @Test
  void createDelegation_RejectsOverlappingDelegation() {
    stubUsers(user(MANAGER_ID, "ROLE_MANAGER"), user(DEPUTY_ID, "ROLE_EMPLOYEE"));
    when(delegationData.existsOverlapping(MANAGER_ID, START, END)).thenReturn(true);

    assertThrows(ManagerDelegationException.Overlapping.class, () -> service.createDelegation(
        new CreateManagerDelegationRequest(null, DEPUTY_ID, START, END, null), MANAGER_ID));
    verify(delegationData, never()).save(any());
  }

  @Test
  void revokeDelegation_ManagerRevokesOwnDelegation() {
    User manager = user(MANAGER_ID, "ROLE_MANAGER");
    ManagerDelegation d = delegation(manager, user(DEPUTY_ID), TODAY, END);
    when(delegationData.findById(100L)).thenReturn(Optional.of(d));
    stubUsers(manager);
    when(delegationData.save(d)).thenReturn(d);

    ManagerDelegationDto dto = service.revokeDelegation(100L, MANAGER_ID);

    assertNotNull(d.getRevokedAt());
    assertEquals("REVOKED", dto.status());
    ArgumentCaptor<ManagerDelegationEvent> captor = ArgumentCaptor.forClass(ManagerDelegationEvent.class);
    verify(eventPublisher).publishEvent(captor.capture());
    assertEquals("REVOKED", captor.getValue().action());
  }

  @Test
  void revokeDelegation_HrCanRevokeAnyDelegation() {
    ManagerDelegation d = delegation(user(MANAGER_ID, "ROLE_MANAGER"), user(DEPUTY_ID), START, END);
    when(delegationData.findById(100L)).thenReturn(Optional.of(d));
    stubUsers(user(HR_ID, "ROLE_HR"));
    when(delegationData.save(d)).thenReturn(d);

    service.revokeDelegation(100L, HR_ID);

    assertNotNull(d.getRevokedAt());
  }

  @Test
  void revokeDelegation_OtherManagerCannotRevoke() {
    ManagerDelegation d = delegation(user(MANAGER_ID, "ROLE_MANAGER"), user(DEPUTY_ID), START, END);
    when(delegationData.findById(100L)).thenReturn(Optional.of(d));
    stubUsers(user(OTHER_MANAGER_ID, "ROLE_MANAGER"));

    assertThrows(ManagerDelegationException.Forbidden.class,
        () -> service.revokeDelegation(100L, OTHER_MANAGER_ID));
  }

  @Test
  void revokeDelegation_RejectsExpiredDelegation() {
    User manager = user(MANAGER_ID, "ROLE_MANAGER");
    ManagerDelegation d = delegation(manager, user(DEPUTY_ID), TODAY.minusDays(10), TODAY.minusDays(1));
    when(delegationData.findById(100L)).thenReturn(Optional.of(d));
    stubUsers(manager);

    assertThrows(ManagerDelegationException.AlreadyFinished.class,
        () -> service.revokeDelegation(100L, MANAGER_ID));
  }

  @Test
  void revokeDelegation_RejectsAlreadyRevoked() {
    User manager = user(MANAGER_ID, "ROLE_MANAGER");
    ManagerDelegation d = delegation(manager, user(DEPUTY_ID), START, END);
    d.setRevokedAt(LocalDateTime.now());
    when(delegationData.findById(100L)).thenReturn(Optional.of(d));
    stubUsers(manager);

    assertThrows(ManagerDelegationException.AlreadyFinished.class,
        () -> service.revokeDelegation(100L, MANAGER_ID));
  }

  @Test
  void revokeDelegation_ThrowsWhenNotFound() {
    when(delegationData.findById(100L)).thenReturn(Optional.empty());

    assertThrows(ManagerDelegationException.NotFound.class,
        () -> service.revokeDelegation(100L, MANAGER_ID));
  }

  @Test
  void getRepresentedManagerIds_ReturnsManagersFromActiveDelegations() {
    ManagerDelegation d = delegation(user(MANAGER_ID), user(DEPUTY_ID), TODAY, END);
    when(delegationData.findActiveByDeputy(DEPUTY_ID, TODAY)).thenReturn(List.of(d));

    assertEquals(Set.of(MANAGER_ID), service.getRepresentedManagerIds(DEPUTY_ID));
    assertTrue(service.isActingDeputy(DEPUTY_ID));
  }

  @Test
  void isActingDeputy_FalseWithoutActiveDelegations() {
    when(delegationData.findActiveByDeputy(DEPUTY_ID, TODAY)).thenReturn(List.of());

    assertFalse(service.isActingDeputy(DEPUTY_ID));
  }

  @Test
  void isActiveDeputyOf_DelegatesToDataLayerForToday() {
    when(delegationData.isActiveDeputyOf(DEPUTY_ID, MANAGER_ID, TODAY)).thenReturn(true);

    assertTrue(service.isActiveDeputyOf(DEPUTY_ID, MANAGER_ID));
    assertFalse(service.isActiveDeputyOf(null, MANAGER_ID));
  }

  @Test
  void findActiveDeputyId_ReturnsDeputyOfActiveDelegation() {
    ManagerDelegation d = delegation(user(MANAGER_ID), user(DEPUTY_ID), TODAY, END);
    when(delegationData.findActiveByManager(MANAGER_ID, TODAY)).thenReturn(List.of(d));

    assertEquals(Optional.of(DEPUTY_ID), service.findActiveDeputyId(MANAGER_ID));
  }

  @Test
  void statusOf_CoversAllStates() {
    ManagerDelegation d = delegation(user(MANAGER_ID), user(DEPUTY_ID), START, END);

    assertEquals("SCHEDULED", ManagerDelegationService.statusOf(d, TODAY));
    assertEquals("ACTIVE", ManagerDelegationService.statusOf(d, START));
    assertEquals("ACTIVE", ManagerDelegationService.statusOf(d, END));
    assertEquals("EXPIRED", ManagerDelegationService.statusOf(d, END.plusDays(1)));
    d.setRevokedAt(LocalDateTime.now());
    assertEquals("REVOKED", ManagerDelegationService.statusOf(d, START));
  }
}
