package pl.chrono.workforceapi.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pl.chrono.logmanager.dto.OperationLogDto;
import pl.chrono.logmanager.service.LogService;
import pl.chrono.usermanager.service.AuthService;
import java.util.List;

@RestController
@RequestMapping("api/logs")
public class LogController extends BaseController {

  private final LogService logService;

  public LogController(AuthService authService, LogService logService){
    super(authService);
    this.logService = logService;
  }

  @GetMapping("/operational")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<List<OperationLogDto>> getAllLogs() {
    List<OperationLogDto> logs = logService.getAllOperationalLogs();

    return ResponseEntity.ok(logs);
  }
}
