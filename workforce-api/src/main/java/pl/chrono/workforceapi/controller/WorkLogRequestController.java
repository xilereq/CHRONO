package pl.chrono.workforceapi.controller;

import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pl.chrono.timemanager.dto.request.WorkLogCreateRequest;
import pl.chrono.timemanager.dto.request.WorkLogResolveRequest;
import pl.chrono.timemanager.dto.response.WorkLogRequestDto;
import pl.chrono.timemanager.service.WorkLogRequestService;
import pl.chrono.usermanager.service.AuthService;

@RestController
@RequestMapping("/api/work-log-requests")
public class WorkLogRequestController extends BaseController {

  private final WorkLogRequestService requestService;

  public WorkLogRequestController(AuthService authService, WorkLogRequestService requestService) {
    super(authService);
    this.requestService = requestService;
  }

  @PostMapping
  public ResponseEntity<Void> submitRequest(
      @RequestHeader("Authorization") String token,
      @RequestBody WorkLogCreateRequest requestDto) {

    Integer employeeId = extractId(token);
    requestService.submitRequest(employeeId, requestDto);
    return ResponseEntity.status(HttpStatus.CREATED).build();
  }

  @GetMapping("/me")
  public ResponseEntity<List<WorkLogRequestDto>> getMyRequests(
      @RequestHeader("Authorization") String token) {

    Integer employeeId = extractId(token);
    return ResponseEntity.ok(requestService.getEmployeeRequests(employeeId));
  }

  @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
  @GetMapping("/pending")
  public ResponseEntity<List<WorkLogRequestDto>> getPendingRequests(
      @RequestHeader("Authorization") String token) {

    Integer reviewerId = extractId(token);
    return ResponseEntity.ok(requestService.getPendingRequestsForReviewer(reviewerId));
  }

  @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
  @PostMapping("/{id}/approve")
  public ResponseEntity<Void> approveRequest(
      @RequestHeader("Authorization") String token,
      @PathVariable("id") Long requestId,
      @RequestBody(required = false) WorkLogResolveRequest resolveDto) {

    Integer reviewerId = extractId(token);
    requestService.approveRequest(requestId, reviewerId, resolveDto);
    return ResponseEntity.ok().build();
  }

  @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
  @PostMapping("/{id}/reject")
  public ResponseEntity<Void> rejectRequest(
      @RequestHeader("Authorization") String token,
      @PathVariable("id") Long requestId,
      @RequestBody(required = false) WorkLogResolveRequest resolveDto) {

    Integer reviewerId = extractId(token);
    requestService.rejectRequest(requestId, reviewerId, resolveDto);
    return ResponseEntity.ok().build();
  }
}
