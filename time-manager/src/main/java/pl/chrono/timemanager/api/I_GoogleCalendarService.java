package pl.chrono.timemanager.api;

import pl.chrono.timemanager.dto.response.GoogleCalendarStatusDto;

public interface I_GoogleCalendarService {

  String getAuthorizationUrl(Integer userId);

  void handleCallback(String code, String state);

  GoogleCalendarStatusDto getStatus(Integer userId);

  void syncApplication(Long applicationId);
}
