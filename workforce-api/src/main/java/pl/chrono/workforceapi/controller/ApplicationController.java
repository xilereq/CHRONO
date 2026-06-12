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
import org.springframework.web.bind.annotation.RestController;
import pl.chrono.applicationmanager.api.I_UserApplication;
import pl.chrono.applicationmanager.dto.request.ChangeApplicationStatusRequest;
import pl.chrono.applicationmanager.dto.request.CreateApplicationRequest;
import pl.chrono.applicationmanager.dto.response.ApplicationResponse;
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
  public ResponseEntity<ApplicationResponse> createApplication(
      @RequestHeader("Authorization") String token,
      @RequestBody CreateApplicationRequest request) {

    Integer employeeId = extractId(token);
    ApplicationResponse createdApplication = applicationService.createApplication(request, employeeId);
    return ResponseEntity.ok(createdApplication);
  }

  @GetMapping("/my")
  @PreAuthorize("hasAnyRole('EMPLOYEE', 'MANAGER', 'HR', 'ADMIN')")
  public ResponseEntity<List<ApplicationResponse>> getMyApplications(
      @RequestHeader("Authorization") String token) {

    Integer myId = extractId(token);
    List<ApplicationResponse> applications = applicationService.getUserApplications(myId, myId);
    return ResponseEntity.ok(applications);
  }

  @GetMapping("/employee/{employeeId}")
  @PreAuthorize("hasAnyRole('MANAGER', 'HR', 'ADMIN')")
  public ResponseEntity<List<ApplicationResponse>> getUserApplications(
      @RequestHeader("Authorization") String token,
      @PathVariable Integer employeeId) {

    Integer requesterId = extractId(token);
    List<ApplicationResponse> applications = applicationService.getUserApplications(employeeId, requesterId);
    return ResponseEntity.ok(applications);
  }

  @PatchMapping("/{applicationId}/status")
  @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
  public ResponseEntity<ApplicationResponse> changeStatus(
      @RequestHeader("Authorization") String token,
      @PathVariable Long applicationId,
      @RequestBody ChangeApplicationStatusRequest request) {

    Integer reviewerId = extractId(token);
    ApplicationResponse updatedApplication = applicationService.changeStatus(
        applicationId, request.newStatus(), reviewerId, request.decisionReason());
    return ResponseEntity.ok(updatedApplication);
  }

  @GetMapping("/manager/pending")
  @PreAuthorize("hasRole('MANAGER')")
  public ResponseEntity<List<ApplicationResponse>> getPendingApplicationsForManager(
      @RequestHeader("Authorization") String token) {

    Integer managerId = extractId(token);
    List<ApplicationResponse> pendingApplications = applicationService.getPendingApplicationsForManager(managerId);
    return ResponseEntity.ok(pendingApplications);
  }

  @GetMapping("/manager/all")
  @PreAuthorize("hasRole('MANAGER')")
  public ResponseEntity<List<ApplicationResponse>> getAllApplicationsForManager(
      @RequestHeader("Authorization") String token) {

    Integer managerId = extractId(token);
    List<ApplicationResponse> allApplications = applicationService.getAllApplicationsForManager(managerId);
    return ResponseEntity.ok(allApplications);
  }

  @GetMapping("/all")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<List<ApplicationResponse>> getAllApplications() {
    List<ApplicationResponse> applications = applicationService.getAllApplications();
    return ResponseEntity.ok(applications);
  }

  @PatchMapping("/{applicationId}/cancel")
  @PreAuthorize("hasAnyRole('EMPLOYEE', 'MANAGER', 'ADMIN')")
  public ResponseEntity<ApplicationResponse> cancelOwnApplication(
      @RequestHeader("Authorization") String token,
      @PathVariable Long applicationId) {

    Integer userId = extractId(token);
    ApplicationResponse cancelled = applicationService.cancelApplication(applicationId, userId);
    return ResponseEntity.ok(cancelled);
  }
}
