package pl.chrono.workforceapi.controller;

import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pl.chrono.dbmanager.model.User;
import pl.chrono.reportmanager.dto.ScheduledReportRequestDto;
import pl.chrono.reportmanager.dto.ScheduledReportResponseDto;
import pl.chrono.reportmanager.service.ScheduledReportService;

@RestController
@RequestMapping("/api/reports/schedules")
@RequiredArgsConstructor
public class ScheduledReportController {

  private final ScheduledReportService scheduledReportService;

  @PostMapping
  @PreAuthorize("isAuthenticated()")
  public ResponseEntity<ScheduledReportResponseDto> createSchedule(
      @AuthenticationPrincipal User currentUser,
      @Valid @RequestBody ScheduledReportRequestDto requestDto) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(scheduledReportService.createSchedule(currentUser, requestDto));
  }

  @GetMapping
  @PreAuthorize("isAuthenticated()")
  public ResponseEntity<List<ScheduledReportResponseDto>> getSchedules(
      @AuthenticationPrincipal User currentUser) {
    return ResponseEntity.ok(scheduledReportService.getSchedules(currentUser));
  }

  @GetMapping("/{scheduleId}")
  @PreAuthorize("isAuthenticated()")
  public ResponseEntity<ScheduledReportResponseDto> getScheduleById(
      @AuthenticationPrincipal User currentUser,
      @PathVariable Long scheduleId) {
    return ResponseEntity.ok(scheduledReportService.getScheduleById(currentUser, scheduleId));
  }

  @PutMapping("/{scheduleId}")
  @PreAuthorize("isAuthenticated()")
  public ResponseEntity<ScheduledReportResponseDto> updateSchedule(
      @AuthenticationPrincipal User currentUser,
      @PathVariable Long scheduleId,
      @Valid @RequestBody ScheduledReportRequestDto requestDto) {
    return ResponseEntity.ok(scheduledReportService.updateSchedule(currentUser, scheduleId, requestDto));
  }

  @PatchMapping("/{scheduleId}/toggle")
  @PreAuthorize("isAuthenticated()")
  public ResponseEntity<ScheduledReportResponseDto> toggleSchedule(
      @AuthenticationPrincipal User currentUser,
      @PathVariable Long scheduleId) {
    return ResponseEntity.ok(scheduledReportService.toggleSchedule(currentUser, scheduleId));
  }

  @PostMapping("/{scheduleId}/trigger")
  @PreAuthorize("isAuthenticated()")
  public ResponseEntity<Void> triggerScheduleNow(
      @AuthenticationPrincipal User currentUser,
      @PathVariable Long scheduleId) {
    scheduledReportService.triggerScheduleNow(currentUser, scheduleId);
    return ResponseEntity.noContent().build();
  }

  @DeleteMapping("/{scheduleId}")
  @PreAuthorize("isAuthenticated()")
  public ResponseEntity<Void> deleteSchedule(
      @AuthenticationPrincipal User currentUser,
      @PathVariable Long scheduleId) {
    scheduledReportService.deleteSchedule(currentUser, scheduleId);
    return ResponseEntity.noContent().build();
  }
}
