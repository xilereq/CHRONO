package pl.chrono.workforceapi.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pl.chrono.dbmanager.model.User;
import pl.chrono.timemanager.dto.WorkLogDto;
import pl.chrono.timemanager.dto.WorkStats;
import pl.chrono.timemanager.service.WorkLogService;
import pl.chrono.usermanager.service.AuthService;

@RestController
@RequestMapping("/api/work-log")
@RequiredArgsConstructor
public class WorkLogController {
  private final WorkLogService workLogService;
  private final AuthService authService;

  @GetMapping("/status")
  public ResponseEntity<Boolean> isWorking(@RequestHeader("Authorization") String token) {
    return ResponseEntity.ok(workLogService.isUserWorking(extractId(token)));
  }

  @PostMapping("/start")
  public ResponseEntity<WorkLogDto> startWork(@RequestHeader("Authorization") String token) {
    return new ResponseEntity<>(workLogService.startWork(extractId(token)), HttpStatus.CREATED);
  }

  @PostMapping("/end")
  public ResponseEntity<WorkLogDto> endWork(@RequestHeader("Authorization") String token) {
    return ResponseEntity.ok(workLogService.endWork(extractId(token)));
  }

  @GetMapping("/active-time")
  public ResponseEntity<String> getActiveDuration(@RequestHeader("Authorization") String token) {
    return ResponseEntity.ok(workLogService.getActiveWorkTime(extractId(token)));
  }

  @GetMapping("/monthly-stats")
  public ResponseEntity<WorkStats> getMonthlyStats(@RequestHeader("Authorization") String token) {
    return ResponseEntity.ok(workLogService.getMonthlyStats(extractId(token)));
  }

  private Integer extractId(String header) {
    return authService.extractUserIdFromToken(header.substring(7));
  }
}
