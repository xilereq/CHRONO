package pl.chrono.timemanager.ports;

import java.time.LocalDate;

public interface I_CalendarProvider {

  String buildAuthorizationUrl(String state);

  String exchangeCodeForRefreshToken(String code);

  void createAllDayEvent(
      String refreshToken,
      String eventId,
      String title,
      LocalDate startDate,
      LocalDate endDateInclusive);
}
