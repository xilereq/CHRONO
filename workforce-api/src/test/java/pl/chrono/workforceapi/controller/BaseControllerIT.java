package pl.chrono.workforceapi.controller;

import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.when;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import pl.chrono.timemanager.service.LeaveLogService;
import pl.chrono.usermanager.security.JwtService;
import pl.chrono.usermanager.service.AuthService;
import pl.chrono.workforceapi.TestApiConfig;

@WebMvcTest(LeaveLogController.class)
@Import(TestApiConfig.class)
@AutoConfigureMockMvc(addFilters = false)
class BaseControllerIT {

  private static final String BEARER_TOKEN = "Bearer valid.jwt.token";
  private static final Integer USER_ID = 42;
  @Autowired
  private MockMvc mockMvc;
  @MockitoBean
  private LeaveLogService leaveLogService;
  @MockitoBean
  private AuthService authService;
  @MockitoBean
  private JwtService jwtService;
  @MockitoBean
  private UserDetailsService userDetailsService;

  @Test
  void extractIdTest() throws Exception {
    when(authService.extractUserIdFromToken("valid.jwt.token")).thenReturn(USER_ID);
    when(leaveLogService.isUserOnLeave(USER_ID)).thenReturn(false);

    mockMvc.perform(get("/api/leave-log/status").header("Authorization", BEARER_TOKEN))
        .andExpect(status().isOk());
  }

  @Test
  void extractIdValidTokenOkTest() throws Exception {
    when(authService.extractUserIdFromToken("valid.jwt.token")).thenReturn(USER_ID);
    when(leaveLogService.isUserOnLeave(USER_ID)).thenReturn(true);

    mockMvc.perform(get("/api/leave-log/status").header("Authorization", BEARER_TOKEN))
        .andExpect(status().isOk());
  }

  @Test
  void extractId400Test() throws Exception {
    mockMvc.perform(get("/api/leave-log/status")).andExpect(status().isBadRequest());
  }
}
