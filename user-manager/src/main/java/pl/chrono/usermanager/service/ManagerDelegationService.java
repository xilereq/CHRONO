package pl.chrono.usermanager.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.chrono.dbmanager.model.ManagerDelegation;
import pl.chrono.dbmanager.model.User;
import pl.chrono.dbmanager.ports.I_ManagerDelegationData;
import pl.chrono.dbmanager.ports.I_UserData;
import pl.chrono.logmanager.audit.LogActivity;
import pl.chrono.usermanager.api.I_ManagerDelegationService;
import pl.chrono.usermanager.dto.request.CreateManagerDelegationRequest;
import pl.chrono.usermanager.dto.response.ManagerDelegationDto;
import pl.chrono.usermanager.event.ManagerDelegationEvent;
import pl.chrono.usermanager.exception.ManagerDelegationException;

@Service
@RequiredArgsConstructor
public class ManagerDelegationService implements I_ManagerDelegationService {

  static final String ROLE_MANAGER = "ROLE_MANAGER";
  private static final Set<String> PRIVILEGED_ROLES = Set.of("ROLE_ADMIN", "ROLE_HR");

  private final I_ManagerDelegationData delegationData;
  private final I_UserData userData;
  private final ApplicationEventPublisher eventPublisher;

  @Override
  @Transactional
  @LogActivity(action = "CREATE_MANAGER_DELEGATION", details = "A manager's permissions were delegated to a deputy")
  public ManagerDelegationDto createDelegation(CreateManagerDelegationRequest request, Integer requesterId) {
    User requester = findUser(requesterId);
    boolean privileged = hasAnyRole(requester, PRIVILEGED_ROLES);

    Integer managerId = request.managerId() != null ? request.managerId() : requesterId;
    if (!privileged && !managerId.equals(requesterId)) {
      throw new ManagerDelegationException.Forbidden(
          "Only ADMIN or HR can create a delegation on behalf of another manager.");
    }

    User manager = findUser(managerId);
    if (!hasAnyRole(manager, Set.of(ROLE_MANAGER))) {
      throw new ManagerDelegationException.NotAManager(managerId);
    }

    validatePeriod(request.startDate(), request.endDate());

    User deputy = findUser(request.deputyId());
    validateDeputy(manager, deputy, request.startDate(), request.endDate());

    if (delegationData.existsOverlapping(managerId, request.startDate(), request.endDate())) {
      throw new ManagerDelegationException.Overlapping();
    }

    ManagerDelegation delegation = new ManagerDelegation();
    delegation.setManager(manager);
    delegation.setDeputy(deputy);
    delegation.setStartDate(request.startDate());
    delegation.setEndDate(request.endDate());
    delegation.setReason(request.reason());
    delegation.setCreatedBy(requester);
    delegation.setCreatedAt(LocalDateTime.now());

    ManagerDelegation saved = delegationData.save(delegation);
    publishEvent(saved, "CREATED");
    return toDto(saved);
  }

  @Override
  @Transactional
  @LogActivity(action = "REVOKE_MANAGER_DELEGATION", details = "A manager delegation was revoked")
  public ManagerDelegationDto revokeDelegation(Long delegationId, Integer requesterId) {
    ManagerDelegation delegation = delegationData.findById(delegationId)
        .orElseThrow(() -> new ManagerDelegationException.NotFound(delegationId));
    User requester = findUser(requesterId);

    boolean isOwner = delegation.getManager().getUserId().equals(requesterId);
    if (!isOwner && !hasAnyRole(requester, PRIVILEGED_ROLES)) {
      throw new ManagerDelegationException.Forbidden(
          "Only the delegating manager, ADMIN or HR can revoke this delegation.");
    }

    if (delegation.isRevoked() || delegation.getEndDate().isBefore(LocalDate.now())) {
      throw new ManagerDelegationException.AlreadyFinished();
    }

    delegation.setRevokedAt(LocalDateTime.now());
    ManagerDelegation saved = delegationData.save(delegation);
    publishEvent(saved, "REVOKED");
    return toDto(saved);
  }

  @Override
  @Transactional(readOnly = true)
  public List<ManagerDelegationDto> getDelegationsGivenBy(Integer managerId) {
    return delegationData.findAllByManager(managerId).stream().map(this::toDto).toList();
  }

  @Override
  @Transactional(readOnly = true)
  public List<ManagerDelegationDto> getDelegationsReceivedBy(Integer deputyId) {
    return delegationData.findAllByDeputy(deputyId).stream().map(this::toDto).toList();
  }

  @Override
  @Transactional(readOnly = true)
  public List<ManagerDelegationDto> getAllDelegations() {
    return delegationData.findAll().stream().map(this::toDto).toList();
  }

  @Override
  @Transactional(readOnly = true)
  public boolean isActiveDeputyOf(Integer deputyId, Integer managerId) {
    if (deputyId == null || managerId == null) {
      return false;
    }
    return delegationData.isActiveDeputyOf(deputyId, managerId, LocalDate.now());
  }

  @Override
  @Transactional(readOnly = true)
  public boolean isActingDeputy(Integer userId) {
    return !getRepresentedManagerIds(userId).isEmpty();
  }

  @Override
  @Transactional(readOnly = true)
  public Set<Integer> getRepresentedManagerIds(Integer userId) {
    if (userId == null) {
      return Set.of();
    }
    return delegationData.findActiveByDeputy(userId, LocalDate.now()).stream()
        .map(d -> d.getManager().getUserId())
        .collect(Collectors.toCollection(LinkedHashSet::new));
  }

  @Override
  @Transactional(readOnly = true)
  public Optional<Integer> findActiveDeputyId(Integer managerId) {
    if (managerId == null) {
      return Optional.empty();
    }
    return delegationData.findActiveByManager(managerId, LocalDate.now()).stream()
        .map(d -> d.getDeputy().getUserId())
        .findFirst();
  }

  private void validatePeriod(LocalDate startDate, LocalDate endDate) {
    if (endDate.isBefore(startDate)) {
      throw new ManagerDelegationException.InvalidPeriod("End date must not be before start date.");
    }
    if (startDate.isBefore(LocalDate.now())) {
      throw new ManagerDelegationException.InvalidPeriod("Start date must not be in the past.");
    }
  }

  private void validateDeputy(User manager, User deputy, LocalDate startDate, LocalDate endDate) {
    if (deputy.getUserId().equals(manager.getUserId())) {
      throw new ManagerDelegationException.InvalidDeputy("A manager cannot be their own deputy.");
    }
    if (!deputy.isEmployedWithin(startDate, endDate)) {
      throw new ManagerDelegationException.InvalidDeputy(
          "The deputy must be active and employed for the whole delegation period.");
    }
  }

  private User findUser(Integer userId) {
    return userData.findById(userId)
        .orElseThrow(() -> new ManagerDelegationException.UserNotFound(userId));
  }

  private static boolean hasAnyRole(User user, Set<String> roleNames) {
    return user.getRoles().stream().anyMatch(role -> roleNames.contains(role.getName()));
  }

  private void publishEvent(ManagerDelegation delegation, String action) {
    eventPublisher.publishEvent(new ManagerDelegationEvent(
        delegation.getId(),
        delegation.getManager().getUserId(),
        fullName(delegation.getManager()),
        delegation.getDeputy().getUserId(),
        fullName(delegation.getDeputy()),
        delegation.getStartDate(),
        delegation.getEndDate(),
        action
    ));
  }

  private ManagerDelegationDto toDto(ManagerDelegation d) {
    return new ManagerDelegationDto(
        d.getId(),
        d.getManager().getUserId(),
        fullName(d.getManager()),
        d.getDeputy().getUserId(),
        fullName(d.getDeputy()),
        d.getStartDate(),
        d.getEndDate(),
        d.getReason(),
        statusOf(d, LocalDate.now()),
        d.getCreatedBy() != null ? d.getCreatedBy().getUserId() : null,
        d.getCreatedAt(),
        d.getRevokedAt()
    );
  }

  static String statusOf(ManagerDelegation d, LocalDate today) {
    if (d.isRevoked()) {
      return "REVOKED";
    }
    if (today.isBefore(d.getStartDate())) {
      return "SCHEDULED";
    }
    if (today.isAfter(d.getEndDate())) {
      return "EXPIRED";
    }
    return "ACTIVE";
  }

  private static String fullName(User user) {
    return user.getName() + " " + user.getSurname();
  }
}
