package pl.chrono.workforceapi.controller;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pl.chrono.timemanager.dto.request.AssignHolidayRequest;
import pl.chrono.timemanager.dto.request.AssignPolicyRequest;
import pl.chrono.timemanager.dto.request.CreatePolicyRequest;
import pl.chrono.timemanager.dto.response.HolidayDto;
import pl.chrono.timemanager.dto.response.PolicyDto;
import pl.chrono.timemanager.service.PolicyConfigurationService;
import pl.chrono.timemanager.service.PolicyManagementService;
import pl.chrono.usermanager.service.AuthService;
import java.util.List;

@RestController
@RequestMapping("/api/policies")
public class PolicyController extends BaseController {

  private final PolicyConfigurationService configService;
  private final PolicyManagementService managementService;

  public PolicyController(AuthService authService,
                          PolicyConfigurationService configService,
                          PolicyManagementService managementService) {
    super(authService);
    this.configService = configService;
    this.managementService = managementService;
  }

  @PostMapping
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<PolicyDto> createPolicy(@Valid @RequestBody CreatePolicyRequest request) {
    return ResponseEntity.ok(configService.createPolicy(request));
  }

  @PostMapping("/{userId}/assign")
  @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
  public ResponseEntity<Void> assignPolicy(
      @PathVariable Integer userId,
      @Valid @RequestBody AssignPolicyRequest request) {

    managementService.assignPolicyToUser(userId, request);

    return ResponseEntity.noContent().build();
  }

  @GetMapping
  @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
  public ResponseEntity<List<PolicyDto>> getAllPolicies() {
    return ResponseEntity.ok(configService.getAllPolicies());
  }

  @GetMapping("/{policyId}")
  @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
  public ResponseEntity<PolicyDto> getPolicyById(@PathVariable Integer policyId) {
    return ResponseEntity.ok(configService.getPolicyById(policyId));
  }

  @GetMapping("/{policyId}/holidays")
  @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
  public ResponseEntity<List<HolidayDto>> getHolidaysForPolicy(@PathVariable Integer policyId) {
    return ResponseEntity.ok(configService.getHolidaysForPolicy(policyId));
  }

  @DeleteMapping("/{policyId}")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<Void> deletePolicy(@PathVariable Integer policyId) {
    configService.deletePolicy(policyId);
    return ResponseEntity.noContent().build();
  }

  @PostMapping("/{policyId}/holidays")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<Void> assignHolidayToPolicy(
      @PathVariable Integer policyId,
      @Valid @RequestBody AssignHolidayRequest request) {

    configService.assignHolidayToPolicy(policyId, request);
    return ResponseEntity.noContent().build();
  }

  @DeleteMapping("/{policyId}/holidays/{holidayId}")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<Void> removeHolidayFromPolicy(
      @PathVariable Integer policyId,
      @PathVariable Integer holidayId) {

    configService.removeHolidayFromPolicy(policyId, holidayId);
    return ResponseEntity.noContent().build();
  }

}
