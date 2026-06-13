package pl.chrono.workforceapi.controller;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pl.chrono.timemanager.dto.response.WorkLogDto;
import pl.chrono.timemanager.dto.response.WorkStats;
import pl.chrono.timemanager.service.WorkLogService;
import pl.chrono.usermanager.service.AuthService;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/work-log")
public class WorkLogController extends BaseController {
  private final WorkLogService workLogService;

  public WorkLogController(AuthService authService, WorkLogService workLogService) {
    super(authService);
    this.workLogService = workLogService;
  }

  @GetMapping("/by-date")
  public ResponseEntity<List<WorkLogDto>> getWorkLogsByDate(
      @RequestHeader("Authorization") String token,
      @RequestParam("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

    return ResponseEntity.ok(workLogService.getWorkLogsByDate(extractId(token), date));
  }

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

  @GetMapping("/today-target")
  public ResponseEntity<String> getTodayTarget(@RequestHeader("Authorization") String token) {
    return ResponseEntity.ok(workLogService.getTodayTarget(extractId(token)));
  }
}
