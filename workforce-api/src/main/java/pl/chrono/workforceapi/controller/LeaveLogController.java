package pl.chrono.workforceapi.controller;


import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pl.chrono.timemanager.api.I_LeaveLogService;
import pl.chrono.timemanager.dto.response.LeaveLogDto;
import pl.chrono.usermanager.service.AuthService;

@RestController
@RequestMapping("/api/leave-log")
public class LeaveLogController extends BaseController {
  private final I_LeaveLogService leaveLogService;

  public LeaveLogController(AuthService authService, I_LeaveLogService leaveLogService) {
    super(authService);
    this.leaveLogService = leaveLogService;
  }

  @GetMapping("/status")
  public ResponseEntity<Boolean> isWorking(@RequestHeader("Authorization") String token) {
    return ResponseEntity.ok(leaveLogService.isUserOnLeave(extractId(token)));
  }

  @PostMapping("/start")
  public ResponseEntity<LeaveLogDto> startWork(@RequestHeader("Authorization") String token) {
    return new ResponseEntity<>(leaveLogService.startLeave(extractId(token)), HttpStatus.CREATED);
  }

  @PostMapping("/end")
  public ResponseEntity<LeaveLogDto> endWork(@RequestHeader("Authorization") String token) {
    return ResponseEntity.ok(leaveLogService.endLeave(extractId(token)));
  }
}
