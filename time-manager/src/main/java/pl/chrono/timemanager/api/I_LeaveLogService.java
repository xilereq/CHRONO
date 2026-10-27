package pl.chrono.timemanager.api;

import pl.chrono.timemanager.dto.response.LeaveLogDto;

public interface I_LeaveLogService {
  LeaveLogDto startLeave(Integer userId);

  LeaveLogDto endLeave(Integer userId);

  boolean isUserOnLeave(Integer userId);
}
