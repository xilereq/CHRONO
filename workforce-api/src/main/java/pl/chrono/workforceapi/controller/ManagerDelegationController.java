package pl.chrono.workforceapi.controller;

import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pl.chrono.usermanager.api.I_ManagerDelegationService;
import pl.chrono.usermanager.dto.request.CreateManagerDelegationRequest;
import pl.chrono.usermanager.dto.response.ManagerDelegationDto;
import pl.chrono.usermanager.service.AuthService;

@RestController
@RequestMapping("/api/delegations")
public class ManagerDelegationController extends BaseController {

  private final I_ManagerDelegationService delegationService;

  public ManagerDelegationController(AuthService authService,
                                     I_ManagerDelegationService delegationService) {
    super(authService);
    this.delegationService = delegationService;
  }

  @PostMapping
  @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN', 'HR')")
  public ResponseEntity<ManagerDelegationDto> createDelegation(
      @RequestHeader("Authorization") String token,
      @Valid @RequestBody CreateManagerDelegationRequest request) {

    Integer requesterId = extractId(token);
    ManagerDelegationDto created = delegationService.createDelegation(request, requesterId);
    return ResponseEntity.status(HttpStatus.CREATED).body(created);
  }

  @PatchMapping("/{delegationId}/revoke")
  @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN', 'HR')")
  public ResponseEntity<ManagerDelegationDto> revokeDelegation(
      @RequestHeader("Authorization") String token,
      @PathVariable Long delegationId) {

    Integer requesterId = extractId(token);
    return ResponseEntity.ok(delegationService.revokeDelegation(delegationId, requesterId));
  }

  @GetMapping("/given")
  @PreAuthorize("hasRole('MANAGER')")
  public ResponseEntity<List<ManagerDelegationDto>> getGivenDelegations(
      @RequestHeader("Authorization") String token) {

    return ResponseEntity.ok(delegationService.getDelegationsGivenBy(extractId(token)));
  }

  @GetMapping("/received")
  @PreAuthorize("hasAnyRole('EMPLOYEE', 'MANAGER', 'HR', 'ADMIN')")
  public ResponseEntity<List<ManagerDelegationDto>> getReceivedDelegations(
      @RequestHeader("Authorization") String token) {

    return ResponseEntity.ok(delegationService.getDelegationsReceivedBy(extractId(token)));
  }

  @GetMapping
  @PreAuthorize("hasAnyRole('ADMIN', 'HR')")
  public ResponseEntity<List<ManagerDelegationDto>> getAllDelegations() {
    return ResponseEntity.ok(delegationService.getAllDelegations());
  }
}
