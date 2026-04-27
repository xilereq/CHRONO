package pl.chrono.workforceapi.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pl.chrono.applicationmanager.api.I_UserApplication;
import pl.chrono.applicationmanager.dto.ApplicationDTO;

import java.util.List;

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
    return ResponseEntity.ok(createdApplication); // Zwracamy status 200 OK i utworzony wniosek
  }

  // GET http://localhost:8080/api/applications/employee/5
  @GetMapping("/employee/{employeeId}")
  public ResponseEntity<List<ApplicationDTO>> getUserApplications(@PathVariable Integer employeeId) {

    List<ApplicationDTO> applications = applicationService.getUserApplications(employeeId);
    return ResponseEntity.ok(applications);
  }

  // PATCH http://localhost:8080/api/applications/12/status?newStatus=APPROVED&reviewerId=3
  @PatchMapping("/{applicationId}/status")
  public ResponseEntity<ApplicationDTO> changeStatus(
    @PathVariable Long applicationId,
    @RequestParam String newStatus,
    @RequestParam Integer reviewerId) {

    ApplicationDTO updatedApplication = applicationService.changeStatus(applicationId, newStatus, reviewerId);
    return ResponseEntity.ok(updatedApplication);
  }
}
