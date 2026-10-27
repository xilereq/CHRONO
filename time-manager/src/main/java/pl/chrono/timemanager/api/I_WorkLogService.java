package pl.chrono.timemanager.api;

import java.time.LocalDate;
import java.util.List;
import pl.chrono.dbmanager.model.enums.WorkLocationType;
import pl.chrono.timemanager.dto.response.WorkLogDto;
import pl.chrono.timemanager.dto.response.WorkStats;

public interface I_WorkLogService {
  List<WorkLogDto> getWorkLogsByDate(Integer userId, LocalDate date);

  WorkLogDto startWork(Integer userId, WorkLocationType location);

  void cancelWork(Integer userId);

  WorkLogDto endWork(Integer userId);

  boolean isUserWorking(Integer userId);

  String getActiveWorkTime(Integer userId);

  WorkStats getMonthlyStats(Integer userId);

  String getTodayTarget(Integer userId);
}
