package pl.chrono.dbmanager.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pl.chrono.dbmanager.model.LeaveLog;
import pl.chrono.dbmanager.repository.LeaveLogRepository;

@RestController
@RequestMapping("/api/leave_log")
public class LeaveLogController extends BaseController<LeaveLog, Integer> {

  private final LeaveLogRepository leaveLogRepository;

  public LeaveLogController(LeaveLogRepository leaveLogRepository) {
    super(leaveLogRepository);
    this.leaveLogRepository = leaveLogRepository;
  }
}
