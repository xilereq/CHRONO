package pl.chrono.workforceapi.controller;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pl.chrono.timemanager.dto.request.AssignPolicyRequest;
import pl.chrono.timemanager.dto.request.CreatePolicyRequest;
import pl.chrono.timemanager.dto.response.PolicyDto;
import pl.chrono.timemanager.service.PolicyConfigurationService;
import pl.chrono.timemanager.service.PolicyManagementService;
import pl.chrono.usermanager.service.AuthService;

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
}
