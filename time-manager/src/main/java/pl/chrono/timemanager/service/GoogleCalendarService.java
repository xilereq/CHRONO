package pl.chrono.timemanager.service;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.chrono.dbmanager.model.Application;
import pl.chrono.dbmanager.model.GoogleCredential;
import pl.chrono.dbmanager.model.User;
import pl.chrono.dbmanager.model.enums.ApplicationStatus;
import pl.chrono.dbmanager.model.enums.ApplicationType;
import pl.chrono.dbmanager.ports.I_GoogleCalendarData;
import pl.chrono.timemanager.api.I_GoogleCalendarService;
import pl.chrono.timemanager.dto.response.GoogleCalendarStatusDto;
import pl.chrono.timemanager.exception.GoogleCalendarException;
import pl.chrono.timemanager.ports.I_CalendarProvider;

@Slf4j
@Service
@RequiredArgsConstructor
public class GoogleCalendarService implements I_GoogleCalendarService {

  private static final Duration STATE_TTL = Duration.ofMinutes(10);
  private static final String EVENT_ID_PREFIX = "chronoapp";
  private static final String TITLE_HOLIDAY_LEAVE = "Urlop";
  private static final String TITLE_HOME_OFFICE = "Praca zdalna";

  private final I_GoogleCalendarData googleCalendarData;
  private final I_CalendarProvider calendarProvider;
  private final TokenEncryptionService tokenEncryptionService;

  private final Map<String, OAuthState> pendingStates = new ConcurrentHashMap<>();

  @Override
  public String getAuthorizationUrl(Integer userId) {
    Instant now = Instant.now();
    pendingStates.entrySet().removeIf(entry -> now.isAfter(entry.getValue().expiresAt()));

    String state = UUID.randomUUID().toString();
    pendingStates.put(state, new OAuthState(userId, now.plus(STATE_TTL)));
    return calendarProvider.buildAuthorizationUrl(state);
  }

  @Override
  public void handleCallback(String code, String state) {
    if (state == null || state.isBlank()) {
      throw new GoogleCalendarException.InvalidState();
    }

    OAuthState oauthState = pendingStates.remove(state);
    if (oauthState == null || Instant.now().isAfter(oauthState.expiresAt())) {
      throw new GoogleCalendarException.InvalidState();
    }

    String refreshToken = calendarProvider.exchangeCodeForRefreshToken(code);
    if (refreshToken == null || refreshToken.isBlank()) {
      throw new GoogleCalendarException.MissingRefreshToken();
    }

    Integer userId = oauthState.userId();
    GoogleCredential credential = googleCalendarData.findCredentialByUserId(userId)
        .orElseGet(() -> {
          GoogleCredential newCredential = new GoogleCredential();
          User user = new User();
          user.setUserId(userId);
          newCredential.setUser(user);
          return newCredential;
        });

    credential.setEncryptedRefreshToken(tokenEncryptionService.encrypt(refreshToken));
    credential.setConnectedAt(LocalDateTime.now());
    googleCalendarData.saveCredential(credential);

    List<Application> applications = googleCalendarData.findSyncableApplications(
        userId, LocalDate.now());
    for (Application application : applications) {
      try {
        String title = resolveEventTitle(application.getType());
        if (title != null) {
          calendarProvider.createAllDayEvent(
              refreshToken,
              EVENT_ID_PREFIX + application.getId(),
              title,
              application.getStartDate(),
              application.getEndDate()
          );
        }
      } catch (Exception e) {
        log.warn("Failed to sync application {} for user {}: {}",
            application.getId(), userId, e.getMessage());
      }
    }
  }

  @Override
  public GoogleCalendarStatusDto getStatus(Integer userId) {
    boolean connected = googleCalendarData.findCredentialByUserId(userId).isPresent();
    return new GoogleCalendarStatusDto(connected);
  }

  @Override
  @Transactional(readOnly = true)
  public void syncApplication(Long applicationId) {
    Optional<Application> applicationOpt = googleCalendarData.findApplicationById(applicationId);
    if (applicationOpt.isEmpty()) {
      return;
    }

    Application application = applicationOpt.get();
    if (application.getStatus() != ApplicationStatus.APPROVED) {
      return;
    }

    String title = resolveEventTitle(application.getType());
    if (title == null) {
      return;
    }

    Integer userId = application.getEmployee().getUserId();
    Optional<GoogleCredential> credentialOpt = googleCalendarData.findCredentialByUserId(userId);
    if (credentialOpt.isEmpty()) {
      return;
    }

    String refreshToken = tokenEncryptionService.decrypt(
        credentialOpt.get().getEncryptedRefreshToken());
    calendarProvider.createAllDayEvent(
        refreshToken,
        EVENT_ID_PREFIX + application.getId(),
        title,
        application.getStartDate(),
        application.getEndDate()
    );
  }

  private String resolveEventTitle(ApplicationType type) {
    if (type == ApplicationType.HOLIDAY_LEAVE) {
      return TITLE_HOLIDAY_LEAVE;
    }
    if (type == ApplicationType.HOME_OFFICE) {
      return TITLE_HOME_OFFICE;
    }
    return null;
  }

  private record OAuthState(Integer userId, Instant expiresAt) {
  }
}
