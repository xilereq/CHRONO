package pl.chrono.reportmanager.api;

import java.util.List;
import pl.chrono.reportmanager.dto.ScheduledReportRequestDto;
import pl.chrono.reportmanager.dto.ScheduledReportResponseDto;

public interface I_ScheduledReportService {

  ScheduledReportResponseDto createSchedule(Integer currentUserId, ScheduledReportRequestDto dto);

  ScheduledReportResponseDto updateSchedule(
      Integer currentUserId, Long scheduleId, ScheduledReportRequestDto dto);

  List<ScheduledReportResponseDto> getSchedules(Integer currentUserId);

  ScheduledReportResponseDto getScheduleById(Integer currentUserId, Long scheduleId);

  ScheduledReportResponseDto toggleSchedule(Integer currentUserId, Long scheduleId);

  void deleteSchedule(Integer currentUserId, Long scheduleId);

  void triggerScheduleNow(Integer currentUserId, Long scheduleId);

  void processDueSchedules();
}
