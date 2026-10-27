package pl.chrono.workforceapi.controller;

import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import pl.chrono.timemanager.api.I_GoogleCalendarService;
import pl.chrono.timemanager.dto.response.GoogleCalendarStatusDto;
import pl.chrono.timemanager.exception.GoogleCalendarException;
import pl.chrono.usermanager.security.JwtService;
import pl.chrono.usermanager.service.AuthService;
import pl.chrono.workforceapi.TestApiConfig;

@WebMvcTest(GoogleCalendarController.class)
@Import(TestApiConfig.class)
@AutoConfigureMockMvc(addFilters = false)
class GoogleCalendarControllerIT {

  private static final String BEARER_TOKEN = "Bearer dummy.jwt.token";
  private static final String RAW_TOKEN = "dummy.jwt.token";

  @Autowired
  private MockMvc mockMvc;

  @MockitoBean
  private I_GoogleCalendarService googleCalendarService;

  @MockitoBean
  private AuthService authService;

  @MockitoBean
  private JwtService jwtService;

  @MockitoBean
  private UserDetailsService userDetailsService;

  @Test
  @WithMockUser
  void connectReturnsAuthorizationUrlTest() throws Exception {
    String authUrl = "https://accounts.google.com/o/oauth2/v2/auth?state=test-state";
    when(authService.extractUserIdFromToken(RAW_TOKEN)).thenReturn(10);
    when(googleCalendarService.getAuthorizationUrl(10)).thenReturn(authUrl);

    mockMvc.perform(get("/api/google/calendar/connect").header("Authorization", BEARER_TOKEN))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.url").value(authUrl));

    verify(googleCalendarService).getAuthorizationUrl(10);
  }

  @Test
  @WithMockUser
  void getStatusConnectedTrueTest() throws Exception {
    when(authService.extractUserIdFromToken(RAW_TOKEN)).thenReturn(10);
    when(googleCalendarService.getStatus(10)).thenReturn(new GoogleCalendarStatusDto(true));

    mockMvc.perform(get("/api/google/calendar/status").header("Authorization", BEARER_TOKEN))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.connected").value(true));

    verify(googleCalendarService).getStatus(10);
  }

  @Test
  @WithMockUser
  void getStatusConnectedFalseTest() throws Exception {
    when(authService.extractUserIdFromToken(RAW_TOKEN)).thenReturn(15);
    when(googleCalendarService.getStatus(15)).thenReturn(new GoogleCalendarStatusDto(false));

    mockMvc.perform(get("/api/google/calendar/status").header("Authorization", BEARER_TOKEN))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.connected").value(false));

    verify(googleCalendarService).getStatus(15);
  }

  @Test
  void callbackSuccessRedirectsToConnectedTest() throws Exception {
    doNothing().when(googleCalendarService).handleCallback("auth-code", "state-123");

    mockMvc.perform(get("/api/auth/google/callback")
            .param("code", "auth-code")
            .param("state", "state-123"))
        .andExpect(status().isFound())
        .andExpect(header().string("Location", "http://localhost:3000/ustawienia?google=connected"));

    verify(googleCalendarService).handleCallback("auth-code", "state-123");
  }

  @Test
  void callbackWithErrorParamRedirectsToDeniedTest() throws Exception {
    mockMvc.perform(get("/api/auth/google/callback")
            .param("error", "access_denied")
            .param("state", "state-123"))
        .andExpect(status().isFound())
        .andExpect(header().string("Location", "http://localhost:3000/ustawienia?google=denied"));

    verifyNoInteractions(googleCalendarService);
  }

  @Test
  void callbackServiceExceptionRedirectsToErrorTest() throws Exception {
    doThrow(new GoogleCalendarException.InvalidState())
        .when(googleCalendarService)
        .handleCallback("auth-code", "invalid-state");

    mockMvc.perform(get("/api/auth/google/callback")
            .param("code", "auth-code")
            .param("state", "invalid-state"))
        .andExpect(status().isFound())
        .andExpect(header().string("Location", "http://localhost:3000/ustawienia?google=error"));

    verify(googleCalendarService).handleCallback("auth-code", "invalid-state");
  }
}
