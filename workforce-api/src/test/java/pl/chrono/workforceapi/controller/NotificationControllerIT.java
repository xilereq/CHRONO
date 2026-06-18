package pl.chrono.workforceapi.controller;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import pl.chrono.dbmanager.model.Notification;
import pl.chrono.notificationmanager.service.NotificationService;
import pl.chrono.usermanager.security.JwtService;
import pl.chrono.usermanager.service.AuthService;
import pl.chrono.workforceapi.TestApiConfig;

@WebMvcTest(NotificationController.class)
@Import(TestApiConfig.class)
@AutoConfigureMockMvc(addFilters = false)
class NotificationControllerIT {

  private static final String BEARER_TOKEN = "Bearer dummy.jwt.token";
  private static final Integer USER_ID = 1;

  @Autowired
  private MockMvc mockMvc;

  @MockitoBean
  private NotificationService notificationService;

  @MockitoBean
  private AuthService authService;

  @MockitoBean
  private JwtService jwtService;

  @MockitoBean
  private UserDetailsService userDetailsService;

  @BeforeEach
  void setUp() {
    when(authService.extractUserIdFromToken(anyString())).thenReturn(USER_ID);
  }

  @Test
  void subscribeTest() throws Exception {
    SseEmitter mockEmitter = new SseEmitter();
    when(notificationService.subscribe(USER_ID)).thenReturn(mockEmitter);

    mockMvc.perform(get("/api/notifications/subscribe")
            .header("Authorization", BEARER_TOKEN))
        .andExpect(status().isOk())
        .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_EVENT_STREAM_VALUE));
  }

  @Test
  void getUnreadNotificationsOkTest() throws Exception {
    Notification mockNotification = mock(Notification.class);
    when(notificationService.getUnreadNotifications(USER_ID)).thenReturn(List.of(mockNotification));

    mockMvc.perform(get("/api/notifications/unread")
            .header("Authorization", BEARER_TOKEN))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(1));
  }

  @Test
  void markAsReadOkTest() throws Exception {
    Long notificationId = 15L;
    doNothing().when(notificationService).markAsRead(notificationId);

    mockMvc.perform(patch("/api/notifications/{id}/read", notificationId)
            .header("Authorization", BEARER_TOKEN))
        .andExpect(status().isOk());
  }

  @Test
  void markAsReadNotFoundTest() throws Exception {
    Long invalidNotificationId = 999L;
    doThrow(new IllegalArgumentException("Notification not found"))
        .when(notificationService).markAsRead(invalidNotificationId);

    mockMvc.perform(patch("/api/notifications/{id}/read", invalidNotificationId)
            .header("Authorization", BEARER_TOKEN))
        .andExpect(status().isBadRequest());
  }
}
