package pl.chrono.workforceapi.controller;

import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pl.chrono.applicationmanager.api.I_UserApplication;
import pl.chrono.applicationmanager.dto.ApplicationDTO;
import pl.chrono.usermanager.service.AuthService;

@RestController
@RequestMapping("/api/applications")
public class ApplicationController extends BaseController {
  private final I_UserApplication applicationService;

  public ApplicationController(AuthService authService, I_UserApplication applicationService) {
    super(authService);
    this.applicationService = applicationService;
  }

  @PostMapping("/employee")
  @PreAuthorize("hasAnyRole('EMPLOYEE', 'MANAGER', 'ADMIN')")
  public ResponseEntity<ApplicationDTO> createApplication(
      @RequestHeader("Authorization") String token,
      @RequestBody ApplicationDTO request) {

    Integer employeeId = extractId(token);
    ApplicationDTO createdApplication = applicationService.createApplication(request, employeeId);
    return ResponseEntity.ok(createdApplication);
  }

  @GetMapping("/employee/{employeeId}")
  @PreAuthorize("hasAnyRole('EMPLOYEE', 'MANAGER', 'HR', 'ADMIN')")
  public ResponseEntity<List<ApplicationDTO>> getUserApplications(
      @RequestHeader("Authorization") String token,
      @PathVariable Integer employeeId) {

    Integer requesterId = extractId(token);
    List<ApplicationDTO> applications = applicationService.getUserApplications(employeeId, requesterId);
    return ResponseEntity.ok(applications);
  }

  @PatchMapping("/{applicationId}/status")
  @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
  public ResponseEntity<ApplicationDTO> changeStatus(
      @RequestHeader("Authorization") String token,
      @PathVariable Long applicationId,
      @RequestParam String newStatus,
      @RequestParam String decisionReason) {

    Integer reviewerId = extractId(token);
    ApplicationDTO updatedApplication = applicationService.changeStatus(
        applicationId, newStatus, reviewerId, decisionReason);
    return ResponseEntity.ok(updatedApplication);
  }

  @GetMapping("/manager/pending")
  @PreAuthorize("hasRole('MANAGER')")
  public ResponseEntity<List<ApplicationDTO>> getPendingApplicationsForManager(
      @RequestHeader("Authorization") String token) {

    Integer managerId = extractId(token);
    List<ApplicationDTO> pendingApplications = applicationService.getPendingApplicationsForManager(managerId);
    return ResponseEntity.ok(pendingApplications);
  }

  @GetMapping("/manager/all")
  @PreAuthorize("hasRole('MANAGER')")
  public ResponseEntity<List<ApplicationDTO>> getAllApplicationsForManager(
      @RequestHeader("Authorization") String token) {

    Integer managerId = extractId(token);
    List<ApplicationDTO> allApplications = applicationService.getAllApplicationsForManager(managerId);
    return ResponseEntity.ok(allApplications);
  }
}
