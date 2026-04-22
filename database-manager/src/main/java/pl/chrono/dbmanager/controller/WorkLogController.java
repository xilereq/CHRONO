package pl.chrono.dbmanager.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pl.chrono.dbmanager.model.WorkLog;
import pl.chrono.dbmanager.repository.WorkLogRepository;

@RestController
@RequestMapping("/api/work_log")
public class WorkLogController extends BaseController<WorkLog, Integer> {

  private final WorkLogRepository workLogRepository;

  public WorkLogController(WorkLogRepository workLogRepository) {
    super(workLogRepository);
    this.workLogRepository = workLogRepository;
  }
}
