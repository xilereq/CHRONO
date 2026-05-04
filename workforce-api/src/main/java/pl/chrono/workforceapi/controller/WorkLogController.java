package pl.chrono.workforceapi.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pl.chrono.timemanager.dto.WorkLogDto;
import pl.chrono.timemanager.service.WorkLogService;

@RestController
@RequestMapping("/api/work-log")
@RequiredArgsConstructor
public class WorkLogController {
  private final WorkLogService workLogService;

  @GetMapping("/status/{userId}")
  public ResponseEntity<Boolean> isWorking(@PathVariable Integer userId) {
    return ResponseEntity.ok(workLogService.isUserWorking(userId));
  }

  @PostMapping("/start/{userId}")
  public ResponseEntity<WorkLogDto> startWork(@PathVariable Integer userId) {
    return new ResponseEntity<>(workLogService.startWork(userId), HttpStatus.CREATED);
  }

  @PostMapping("/end/{userId}")
  public ResponseEntity<WorkLogDto> endWork(@PathVariable Integer userId) {
    return ResponseEntity.ok(workLogService.endWork(userId));
  }
}
