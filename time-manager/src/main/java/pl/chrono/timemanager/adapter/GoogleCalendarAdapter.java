package pl.chrono.timemanager.adapter;

import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeRequestUrl;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeTokenRequest;
import com.google.api.client.googleapis.auth.oauth2.GoogleTokenResponse;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.DateTime;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.CalendarScopes;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.EventDateTime;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.UserCredentials;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.time.LocalDate;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import pl.chrono.timemanager.exception.GoogleCalendarException;
import pl.chrono.timemanager.ports.I_CalendarProvider;

@Slf4j
@Component
public class GoogleCalendarAdapter implements I_CalendarProvider {

  private static final String APPLICATION_NAME = "CHRONO";
  private static final String PRIMARY_CALENDAR_ID = "primary";
  private static final String ACCESS_TYPE_OFFLINE = "offline";
  private static final String PROMPT_PARAM = "prompt";
  private static final String PROMPT_CONSENT = "consent";
  private static final int HTTP_CONFLICT = 409;

  @Value("${chrono.google.client-id:}")
  private String clientId;

  @Value("${chrono.google.client-secret:}")
  private String clientSecret;

  @Value("${chrono.google.redirect-uri:}")
  private String redirectUri;

  @Override
  public String buildAuthorizationUrl(String state) {
    validateConfiguration();
    GoogleAuthorizationCodeRequestUrl requestUrl = new GoogleAuthorizationCodeRequestUrl(
        clientId,
        redirectUri,
        List.of(CalendarScopes.CALENDAR_EVENTS))
        .setState(state)
        .setAccessType(ACCESS_TYPE_OFFLINE);
    requestUrl.set(PROMPT_PARAM, PROMPT_CONSENT);
    return requestUrl.build();
  }

  @Override
  public String exchangeCodeForRefreshToken(String code) {
    validateConfiguration();
    try {
      GoogleTokenResponse tokenResponse = new GoogleAuthorizationCodeTokenRequest(
          GoogleNetHttpTransport.newTrustedTransport(),
          GsonFactory.getDefaultInstance(),
          clientId,
          clientSecret,
          code,
          redirectUri)
          .execute();
      return tokenResponse.getRefreshToken();
    } catch (IOException | GeneralSecurityException e) {
      throw new GoogleCalendarException.ExternalApiError(e.getMessage());
    }
  }

  @Override
  public void createAllDayEvent(
      String refreshToken,
      String eventId,
      String title,
      LocalDate startDate,
      LocalDate endDateInclusive) {
    validateConfiguration();
    try {
      UserCredentials credentials = UserCredentials.newBuilder()
          .setClientId(clientId)
          .setClientSecret(clientSecret)
          .setRefreshToken(refreshToken)
          .build();

      Calendar calendar = new Calendar.Builder(
          GoogleNetHttpTransport.newTrustedTransport(),
          GsonFactory.getDefaultInstance(),
          new HttpCredentialsAdapter(credentials))
          .setApplicationName(APPLICATION_NAME)
          .build();

      EventDateTime start = new EventDateTime()
          .setDate(new DateTime(startDate.toString()));
      EventDateTime end = new EventDateTime()
          .setDate(new DateTime(endDateInclusive.plusDays(1).toString()));

      Event event = new Event()
          .setId(eventId)
          .setSummary(title)
          .setStart(start)
          .setEnd(end);

      calendar.events().insert(PRIMARY_CALENDAR_ID, event).execute();
    } catch (GoogleJsonResponseException e) {
      if (e.getStatusCode() == HTTP_CONFLICT) {
        log.debug("Google Calendar event {} already exists (409 Conflict)", eventId);
        return;
      }
      throw new GoogleCalendarException.ExternalApiError(e.getMessage());
    } catch (IOException | GeneralSecurityException e) {
      throw new GoogleCalendarException.ExternalApiError(e.getMessage());
    }
  }

  private void validateConfiguration() {
    if (isBlank(clientId) || isBlank(clientSecret) || isBlank(redirectUri)) {
      throw new GoogleCalendarException.NotConfigured();
    }
  }

  private boolean isBlank(String value) {
    return value == null || value.isBlank();
  }
}
