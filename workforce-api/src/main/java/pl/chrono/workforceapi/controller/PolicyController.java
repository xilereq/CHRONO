package pl.chrono.workforceapi.controller;

import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pl.chrono.timemanager.api.I_PolicyService;
import pl.chrono.timemanager.dto.request.AssignHolidayRequest;
import pl.chrono.timemanager.dto.request.AssignPolicyRequest;
import pl.chrono.timemanager.dto.request.CreatePolicyRequest;
import pl.chrono.timemanager.dto.request.MassAssignHolidayRequest;
import pl.chrono.timemanager.dto.request.MassAssignPolicyRequest;
import pl.chrono.timemanager.dto.response.HolidayDto;
import pl.chrono.timemanager.dto.response.PolicyDto;
import pl.chrono.usermanager.service.AuthService;

@RestController
@RequestMapping("/api/policies")
public class PolicyController extends BaseController {

  private final I_PolicyService policyService;

  public PolicyController(AuthService authService,
                          I_PolicyService policyService) {
    super(authService);
    this.policyService = policyService;
  }

  @PostMapping
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<PolicyDto> createPolicy(@Valid @RequestBody CreatePolicyRequest request) {
    return ResponseEntity.ok(policyService.createPolicy(request));
  }

  @PostMapping("/{userId}/assign")
  @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
  public ResponseEntity<Void> assignPolicy(
      @PathVariable Integer userId,
      @Valid @RequestBody AssignPolicyRequest request) {

    policyService.assignPolicyToUser(userId, request);

    return ResponseEntity.noContent().build();
  }

  @GetMapping
  @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
  public ResponseEntity<List<PolicyDto>> getAllPolicies() {
    return ResponseEntity.ok(policyService.getAllPolicies());
  }

  @GetMapping("/{policyId}")
  @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
  public ResponseEntity<PolicyDto> getPolicyById(@PathVariable Integer policyId) {
    return ResponseEntity.ok(policyService.getPolicyById(policyId));
  }

  @GetMapping("/{policyId}/holidays")
  @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
  public ResponseEntity<List<HolidayDto>> getHolidaysForPolicy(@PathVariable Integer policyId) {
    return ResponseEntity.ok(policyService.getHolidaysForPolicy(policyId));
  }

  @DeleteMapping("/{policyId}")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<Void> deletePolicy(@PathVariable Integer policyId) {
    policyService.deletePolicy(policyId);
    return ResponseEntity.noContent().build();
  }

  @PostMapping("/{policyId}/holidays")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<Void> assignHolidayToPolicy(
      @PathVariable Integer policyId,
      @Valid @RequestBody AssignHolidayRequest request) {

    policyService.assignHolidayToPolicy(policyId, request);
    return ResponseEntity.noContent().build();
  }

  @DeleteMapping("/{policyId}/holidays/{holidayId}")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<Void> removeHolidayFromPolicy(
      @PathVariable Integer policyId,
      @PathVariable Integer holidayId) {

    policyService.removeHolidayFromPolicy(policyId, holidayId);
    return ResponseEntity.noContent().build();
  }

  @PostMapping("/assign-bulk")
  @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
  public ResponseEntity<Void> assignPolicyBulk(
      @Valid @RequestBody MassAssignPolicyRequest request) {

    policyService.assignPolicyToUsersBulk(request);
    return ResponseEntity.noContent().build();
  }

  @PostMapping("/{policyId}/holidays/bulk")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<Void> assignHolidaysToPolicyBulk(
      @PathVariable Integer policyId,
      @Valid @RequestBody MassAssignHolidayRequest request) {

    policyService.assignHolidaysToPolicyBulk(policyId, request);
    return ResponseEntity.noContent().build();
  }
}
