package pl.chrono.timemanager.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import pl.chrono.dbmanager.model.Application;
import pl.chrono.dbmanager.model.GoogleCredential;
import pl.chrono.dbmanager.model.User;
import pl.chrono.dbmanager.model.enums.ApplicationStatus;
import pl.chrono.dbmanager.model.enums.ApplicationType;
import pl.chrono.dbmanager.ports.I_GoogleCalendarData;
import pl.chrono.timemanager.dto.response.GoogleCalendarStatusDto;
import pl.chrono.timemanager.exception.GoogleCalendarException;
import pl.chrono.timemanager.ports.I_CalendarProvider;

@ExtendWith(MockitoExtension.class)
class GoogleCalendarServiceTest {

  @Mock
  private I_GoogleCalendarData googleCalendarData;

  @Mock
  private I_CalendarProvider calendarProvider;

  @Mock
  private TokenEncryptionService tokenEncryptionService;

  @InjectMocks
  private GoogleCalendarService googleCalendarService;

  @Test
  void getAuthorizationUrlAndHandleCallbackSuccessTest() {
    Integer userId = 10;
    ArgumentCaptor<String> stateCaptor = ArgumentCaptor.forClass(String.class);
    when(calendarProvider.buildAuthorizationUrl(stateCaptor.capture()))
        .thenReturn("https://accounts.google.com/o/oauth2/auth?state=test");

    String url = googleCalendarService.getAuthorizationUrl(userId);
    String generatedState = stateCaptor.getValue();

    assertNotNull(url);
    assertNotNull(generatedState);

    Application app1 = createApplication(1L, userId, ApplicationType.HOLIDAY_LEAVE,
        ApplicationStatus.APPROVED, LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 5));
    Application app2 = createApplication(2L, userId, ApplicationType.HOME_OFFICE,
        ApplicationStatus.APPROVED, LocalDate.of(2026, 6, 10), LocalDate.of(2026, 6, 10));

    when(calendarProvider.exchangeCodeForRefreshToken("auth-code")).thenReturn("plain-refresh");
    when( googleCalendarData.findCredentialByUserId(userId)).thenReturn(Optional.empty());
    when(tokenEncryptionService.encrypt("plain-refresh")).thenReturn("encrypted-refresh");
    when(googleCalendarData.findSyncableApplications(eq(userId), any(LocalDate.class)))
        .thenReturn(List.of(app1, app2));

    googleCalendarService.handleCallback("auth-code", generatedState);

    ArgumentCaptor<GoogleCredential> credentialCaptor =
        ArgumentCaptor.forClass(GoogleCredential.class);
    verify(googleCalendarData).saveCredential(credentialCaptor.capture());
    GoogleCredential saved = credentialCaptor.getValue();
    assertEquals(userId, saved.getUser().getUserId());
    assertEquals("encrypted-refresh", saved.getEncryptedRefreshToken());
    assertNotNull(saved.getConnectedAt());

    verify(calendarProvider).createAllDayEvent(
        "plain-refresh", "chronoapp1", "Urlop",
        LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 5));
    verify(calendarProvider).createAllDayEvent(
        "plain-refresh", "chronoapp2", "Praca zdalna",
        LocalDate.of(2026, 6, 10), LocalDate.of(2026, 6, 10));
  }

  @Test
  void handleCallbackUnknownStateThrowsInvalidStateTest() {
    assertThrows(
        GoogleCalendarException.InvalidState.class,
        () -> googleCalendarService.handleCallback("code", "unknown-state")
    );
  }

  @Test
  void handleCallbackReusedStateThrowsInvalidStateTest() {
    Integer userId = 5;
    ArgumentCaptor<String> stateCaptor = ArgumentCaptor.forClass(String.class);
    when(calendarProvider.buildAuthorizationUrl(stateCaptor.capture())).thenReturn("url");
    googleCalendarService.getAuthorizationUrl(userId);
    String state = stateCaptor.getValue();

    when(calendarProvider.exchangeCodeForRefreshToken("code")).thenReturn("refresh-token");
    when(googleCalendarData.findCredentialByUserId(userId)).thenReturn(Optional.empty());
    when(tokenEncryptionService.encrypt("refresh-token")).thenReturn("enc");
    when(googleCalendarData.findSyncableApplications(eq(userId), any(LocalDate.class)))
        .thenReturn(List.of());

    googleCalendarService.handleCallback("code", state);

    assertThrows(
        GoogleCalendarException.InvalidState.class,
        () -> googleCalendarService.handleCallback("code", state)
    );
  }

  @Test
  void handleCallbackMissingRefreshTokenThrowsExceptionTest() {
    ArgumentCaptor<String> stateCaptor = ArgumentCaptor.forClass(String.class);
    when(calendarProvider.buildAuthorizationUrl(stateCaptor.capture())).thenReturn("url");
    googleCalendarService.getAuthorizationUrl(1);

    when(calendarProvider.exchangeCodeForRefreshToken("code")).thenReturn("");

    assertThrows(
        GoogleCalendarException.MissingRefreshToken.class,
        () -> googleCalendarService.handleCallback("code", stateCaptor.getValue())
    );
  }

  @Test
  void handleCallbackSingleSyncFailureDoesNotStopOthersTest() {
    Integer userId = 7;
    ArgumentCaptor<String> stateCaptor = ArgumentCaptor.forClass(String.class);
    when(calendarProvider.buildAuthorizationUrl(stateCaptor.capture())).thenReturn("url");
    googleCalendarService.getAuthorizationUrl(userId);

    Application app1 = createApplication(10L, userId, ApplicationType.HOLIDAY_LEAVE,
        ApplicationStatus.APPROVED, LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 2));
    Application app2 = createApplication(11L, userId, ApplicationType.HOME_OFFICE,
        ApplicationStatus.APPROVED, LocalDate.of(2026, 7, 3), LocalDate.of(2026, 7, 3));

    when(calendarProvider.exchangeCodeForRefreshToken("code")).thenReturn("token");
    when(googleCalendarData.findCredentialByUserId(userId)).thenReturn(Optional.empty());
    when(tokenEncryptionService.encrypt("token")).thenReturn("enc-token");
    when(googleCalendarData.findSyncableApplications(eq(userId), any(LocalDate.class)))
        .thenReturn(List.of(app1, app2));

    doThrow(new GoogleCalendarException.ExternalApiError("API error"))
        .when(calendarProvider)
        .createAllDayEvent(
            eq("token"), eq("chronoapp10"), eq("Urlop"),
            any(LocalDate.class), any(LocalDate.class));

    googleCalendarService.handleCallback("code", stateCaptor.getValue());

    verify(calendarProvider).createAllDayEvent(
        "token", "chronoapp10", "Urlop",
        LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 2));
    verify(calendarProvider).createAllDayEvent(
        "token", "chronoapp11", "Praca zdalna",
        LocalDate.of(2026, 7, 3), LocalDate.of(2026, 7, 3));
  }

  @Test
  void getStatusTest() {
    when(googleCalendarData.findCredentialByUserId(1))
        .thenReturn(Optional.of(new GoogleCredential()));
    when(googleCalendarData.findCredentialByUserId(2))
        .thenReturn(Optional.empty());

    GoogleCalendarStatusDto status1 = googleCalendarService.getStatus(1);
    GoogleCalendarStatusDto status2 = googleCalendarService.getStatus(2);

    assertTrue(status1.connected());
    assertFalse(status2.connected());
  }

  @Test
  void syncApplicationSkipsUnsupportedTypeTest() {
    Application sickLeave = createApplication(50L, 1, ApplicationType.SICK_LEAVE,
        ApplicationStatus.APPROVED, LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 5));
    when(googleCalendarData.findApplicationById(50L)).thenReturn(Optional.of(sickLeave));

    googleCalendarService.syncApplication(50L);

    verifyNoInteractions(calendarProvider, tokenEncryptionService);
    verify(googleCalendarData, never()).findCredentialByUserId(any());
  }

  @Test
  void syncApplicationSkipsUnapprovedApplicationTest() {
    Application pendingApp = createApplication(51L, 1, ApplicationType.HOLIDAY_LEAVE,
        ApplicationStatus.PENDING, LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 5));
    when(googleCalendarData.findApplicationById(51L)).thenReturn(Optional.of(pendingApp));

    googleCalendarService.syncApplication(51L);

    verifyNoInteractions(calendarProvider, tokenEncryptionService);
    verify(googleCalendarData, never()).findCredentialByUserId(any());
  }

  @Test
  void syncApplicationSkipsWhenCredentialMissingTest() {
    Application approvedApp = createApplication(52L, 1, ApplicationType.HOLIDAY_LEAVE,
        ApplicationStatus.APPROVED, LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 5));
    when(googleCalendarData.findApplicationById(52L)).thenReturn(Optional.of(approvedApp));
    when(googleCalendarData.findCredentialByUserId(1)).thenReturn(Optional.empty());

    googleCalendarService.syncApplication(52L);

    verifyNoInteractions(calendarProvider, tokenEncryptionService);
  }

  @Test
  void syncApplicationHolidayLeaveCallsProviderTest() {
    Application holidayApp = createApplication(100L, 3, ApplicationType.HOLIDAY_LEAVE,
        ApplicationStatus.APPROVED, LocalDate.of(2026, 8, 10), LocalDate.of(2026, 8, 14));
    GoogleCredential credential = new GoogleCredential();
    credential.setEncryptedRefreshToken("enc-token");

    when(googleCalendarData.findApplicationById(100L)).thenReturn(Optional.of(holidayApp));
    when(googleCalendarData.findCredentialByUserId(3)).thenReturn(Optional.of(credential));
    when(tokenEncryptionService.decrypt("enc-token")).thenReturn("dec-token");

    googleCalendarService.syncApplication(100L);

    verify(calendarProvider).createAllDayEvent(
        "dec-token", "chronoapp100", "Urlop",
        LocalDate.of(2026, 8, 10), LocalDate.of(2026, 8, 14));
  }

  @Test
  void syncApplicationHomeOfficeCallsProviderTest() {
    Application hoApp = createApplication(101L, 4, ApplicationType.HOME_OFFICE,
        ApplicationStatus.APPROVED, LocalDate.of(2026, 8, 20), LocalDate.of(2026, 8, 21));
    GoogleCredential credential = new GoogleCredential();
    credential.setEncryptedRefreshToken("enc-ho");

    when(googleCalendarData.findApplicationById(101L)).thenReturn(Optional.of(hoApp));
    when(googleCalendarData.findCredentialByUserId(4)).thenReturn(Optional.of(credential));
    when(tokenEncryptionService.decrypt("enc-ho")).thenReturn("dec-ho");

    googleCalendarService.syncApplication(101L);

    verify(calendarProvider).createAllDayEvent(
        "dec-ho", "chronoapp101", "Praca zdalna",
        LocalDate.of(2026, 8, 20), LocalDate.of(2026, 8, 21));
  }

  private Application createApplication(
      Long id,
      Integer userId,
      ApplicationType type,
      ApplicationStatus status,
      LocalDate startDate,
      LocalDate endDate) {
    User employee = new User();
    employee.setUserId(userId);

    Application app = new Application();
    app.setId(id);
    app.setEmployee(employee);
    app.setType(type);
    app.setStatus(status);
    app.setStartDate(startDate);
    app.setEndDate(endDate);
    return app;
  }
}
