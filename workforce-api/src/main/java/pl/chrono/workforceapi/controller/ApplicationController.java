package pl.chrono.workforceapi.controller;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pl.chrono.applicationmanager.api.I_UserApplication;
import pl.chrono.applicationmanager.dto.ApplicationDTO;

@RestController
@RequestMapping("/api/applications")
@RequiredArgsConstructor
public class ApplicationController {
  private final I_UserApplication applicationService;

  @PostMapping("/employee/{employeeId}")
  @PreAuthorize("hasAnyRole('EMPLOYEE', 'MANAGER', 'ADMIN')")
  public ResponseEntity<ApplicationDTO> createApplication(
      @PathVariable Integer employeeId,
      @RequestBody ApplicationDTO request) {

    ApplicationDTO createdApplication = applicationService.createApplication(request, employeeId);
    return ResponseEntity.ok(createdApplication);
  }

  @GetMapping("/employee/{employeeId}")
  @PreAuthorize("hasAnyRole('EMPLOYEE', 'MANAGER', 'HR', 'ADMIN')")
  public ResponseEntity<List<ApplicationDTO>> getUserApplications(
      @PathVariable Integer employeeId) {

    List<ApplicationDTO> applications = applicationService.getUserApplications(employeeId);
    return ResponseEntity.ok(applications);
  }

  @PatchMapping("/{applicationId}/status")
  @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
  public ResponseEntity<ApplicationDTO> changeStatus(
      @PathVariable Long applicationId,
      @RequestParam String newStatus,
      @RequestParam Integer reviewerId,
      @RequestParam String decisionReason) {

    ApplicationDTO updatedApplication = applicationService.changeStatus(
        applicationId, newStatus, reviewerId, decisionReason);
    return ResponseEntity.ok(updatedApplication);
  }

  @GetMapping("/manager/{managerId}/pending")
  @PreAuthorize("hasRole('MANAGER')")
  public ResponseEntity<List<ApplicationDTO>> getPendingApplicationsForManager(
      @PathVariable Integer managerId) {

    List<ApplicationDTO> pendingApplications = applicationService.getPendingApplicationsForManager(managerId);
    return ResponseEntity.ok(pendingApplications);
  }
}
