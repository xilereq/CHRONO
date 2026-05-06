package pl.chrono.workforceapi.controller;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
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

  // POST http://localhost:8080/api/applications/employee/5
  @PostMapping("/employee/{employeeId}")
  public ResponseEntity<ApplicationDTO> createApplication(
      @PathVariable Integer employeeId,
      @RequestBody ApplicationDTO request) {

    ApplicationDTO createdApplication = applicationService.createApplication(request, employeeId);
    return ResponseEntity.ok(createdApplication);
  }

  // GET http://localhost:8080/api/applications/employee/5
  @GetMapping("/employee/{employeeId}")
  public ResponseEntity<List<ApplicationDTO>> getUserApplications(
      @PathVariable Integer employeeId) {

    List<ApplicationDTO> applications = applicationService.getUserApplications(employeeId);
    return ResponseEntity.ok(applications);
  }

  // PATCH http://localhost:8080/api/applications/12/status?newStatus=APPROVED&reviewerId=3
  @PatchMapping("/{applicationId}/status")
  public ResponseEntity<ApplicationDTO> changeStatus(
      @PathVariable Long applicationId,
      @RequestParam String newStatus,
      @RequestParam Integer reviewerId,
      @RequestParam String decisionReason) {

    ApplicationDTO updatedApplication = applicationService.changeStatus(
        applicationId, newStatus, reviewerId, decisionReason);
    return ResponseEntity.ok(updatedApplication);
  }
}
