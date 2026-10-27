package pl.chrono.workforceapi.controller;

import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pl.chrono.reportmanager.api.I_ScheduledReportService;
import pl.chrono.reportmanager.dto.ScheduledReportRequestDto;
import pl.chrono.reportmanager.dto.ScheduledReportResponseDto;
import pl.chrono.usermanager.service.AuthService;

@RestController
@RequestMapping("/api/reports/schedules")
public class ScheduledReportController extends BaseController {

  private final I_ScheduledReportService scheduledReportService;

  public ScheduledReportController(
      AuthService authService, I_ScheduledReportService scheduledReportService) {
    super(authService);
    this.scheduledReportService = scheduledReportService;
  }

  @PostMapping
  @PreAuthorize("isAuthenticated()")
  public ResponseEntity<ScheduledReportResponseDto> createSchedule(
      @RequestHeader("Authorization") String token,
      @Valid @RequestBody ScheduledReportRequestDto requestDto) {
    Integer currentUserId = extractId(token);
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(scheduledReportService.createSchedule(currentUserId, requestDto));
  }

  @GetMapping
  @PreAuthorize("isAuthenticated()")
  public ResponseEntity<List<ScheduledReportResponseDto>> getSchedules(
      @RequestHeader("Authorization") String token) {
    Integer currentUserId = extractId(token);
    return ResponseEntity.ok(scheduledReportService.getSchedules(currentUserId));
  }

  @GetMapping("/{scheduleId}")
  @PreAuthorize("isAuthenticated()")
  public ResponseEntity<ScheduledReportResponseDto> getScheduleById(
      @RequestHeader("Authorization") String token,
      @PathVariable Long scheduleId) {
    Integer currentUserId = extractId(token);
    return ResponseEntity.ok(scheduledReportService.getScheduleById(currentUserId, scheduleId));
  }

  @PutMapping("/{scheduleId}")
  @PreAuthorize("isAuthenticated()")
  public ResponseEntity<ScheduledReportResponseDto> updateSchedule(
      @RequestHeader("Authorization") String token,
      @PathVariable Long scheduleId,
      @Valid @RequestBody ScheduledReportRequestDto requestDto) {
    Integer currentUserId = extractId(token);
    return ResponseEntity.ok(
        scheduledReportService.updateSchedule(currentUserId, scheduleId, requestDto));
  }

  @PatchMapping("/{scheduleId}/toggle")
  @PreAuthorize("isAuthenticated()")
  public ResponseEntity<ScheduledReportResponseDto> toggleSchedule(
      @RequestHeader("Authorization") String token,
      @PathVariable Long scheduleId) {
    Integer currentUserId = extractId(token);
    return ResponseEntity.ok(scheduledReportService.toggleSchedule(currentUserId, scheduleId));
  }

  @PostMapping("/{scheduleId}/trigger")
  @PreAuthorize("isAuthenticated()")
  public ResponseEntity<Void> triggerScheduleNow(
      @RequestHeader("Authorization") String token,
      @PathVariable Long scheduleId) {
    Integer currentUserId = extractId(token);
    scheduledReportService.triggerScheduleNow(currentUserId, scheduleId);
    return ResponseEntity.noContent().build();
  }

  @DeleteMapping("/{scheduleId}")
  @PreAuthorize("isAuthenticated()")
  public ResponseEntity<Void> deleteSchedule(
      @RequestHeader("Authorization") String token,
      @PathVariable Long scheduleId) {
    Integer currentUserId = extractId(token);
    scheduledReportService.deleteSchedule(currentUserId, scheduleId);
    return ResponseEntity.noContent().build();
  }
}
