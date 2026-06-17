package pl.chrono.workforceapi.controller;

import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import pl.chrono.timemanager.dto.response.LeaveLogDto;
import pl.chrono.timemanager.exception.WorkException;
import pl.chrono.timemanager.service.LeaveLogService;
import pl.chrono.usermanager.security.JwtService;
import pl.chrono.usermanager.service.AuthService;
import pl.chrono.workforceapi.TestApiConfig;

@WebMvcTest(LeaveLogController.class)
@Import(TestApiConfig.class)
@AutoConfigureMockMvc(addFilters = false)
class LeaveLogControllerIT {

  private static final String BEARER_TOKEN = "Bearer dummy.jwt.token";
  private static final Integer USER_ID = 10;
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

  @BeforeEach
  void setUp() {
    when(authService.extractUserIdFromToken(anyString())).thenReturn(USER_ID);
  }

  private LeaveLogDto sampleLeaveLogDto(Integer leaveLogId, LocalDateTime start,
                                        LocalDateTime end) {
    LeaveLogDto dto = new LeaveLogDto();
    dto.setLeaveLogId(leaveLogId);
    dto.setUserId(USER_ID);
    dto.setWorkLog(55);
    dto.setStartTime(start);
    dto.setEndTime(end);
    return dto;
  }

  @Test
  void getStatusFalseTest() throws Exception {
    when(leaveLogService.isUserOnLeave(USER_ID)).thenReturn(false);

    mockMvc.perform(get("/api/leave-log/status").header("Authorization", BEARER_TOKEN))
        .andExpect(status().isOk()).andExpect(jsonPath("$").value(false));
  }

  @Test
  void getStatusTrueTest() throws Exception {
    when(leaveLogService.isUserOnLeave(USER_ID)).thenReturn(true);

    mockMvc.perform(get("/api/leave-log/status").header("Authorization", BEARER_TOKEN))
        .andExpect(status().isOk()).andExpect(jsonPath("$").value(true));
  }

  @Test
  void getStatusNoTokenTest() throws Exception {
    mockMvc.perform(get("/api/leave-log/status")).andExpect(status().isBadRequest());
  }

  @Test
  void startLeaveCreatedTest() throws Exception {
    LocalDateTime now = LocalDateTime.now();
    LeaveLogDto dto = sampleLeaveLogDto(1, now, null);
    when(leaveLogService.startLeave(USER_ID)).thenReturn(dto);

    mockMvc.perform(post("/api/leave-log/start").header("Authorization", BEARER_TOKEN))
        .andExpect(status().isCreated()).andExpect(jsonPath("$.leaveLogId").value(1))
        .andExpect(jsonPath("$.userId").value(USER_ID)).andExpect(jsonPath("$.workLog").value(55))
        .andExpect(jsonPath("$.endTime").isEmpty());
  }

  @Test
  void startLeaveNoTokenTest() throws Exception {
    mockMvc.perform(post("/api/leave-log/start")).andExpect(status().isBadRequest());
  }

  @Test
  void startLeaveNoActiveLogTest() throws Exception {
    when(leaveLogService.startLeave(USER_ID)).thenThrow(new WorkException.NoActiveWork(USER_ID));

    mockMvc.perform(post("/api/leave-log/start").header("Authorization", BEARER_TOKEN))
        .andExpect(status().is4xxClientError());
  }

  @Test
  void startLeaveOnLeaveTest() throws Exception {
    when(leaveLogService.startLeave(USER_ID)).thenThrow(new WorkException.LeaveStarted(USER_ID));

    mockMvc.perform(post("/api/leave-log/start").header("Authorization", BEARER_TOKEN))
        .andExpect(status().is4xxClientError());
  }

  @Test
  void endLeaveProperTest() throws Exception {
    LocalDateTime start = LocalDateTime.now().minusHours(1);
    LocalDateTime end = LocalDateTime.now();
    LeaveLogDto dto = sampleLeaveLogDto(1, start, end);
    when(leaveLogService.endLeave(USER_ID)).thenReturn(dto);

    mockMvc.perform(post("/api/leave-log/end").header("Authorization", BEARER_TOKEN))
        .andExpect(status().isOk()).andExpect(jsonPath("$.leaveLogId").value(1))
        .andExpect(jsonPath("$.userId").value(USER_ID))
        .andExpect(jsonPath("$.endTime").isNotEmpty());
  }

  @Test
  void endLeaveNoTokenTest() throws Exception {
    mockMvc.perform(post("/api/leave-log/end")).andExpect(status().isBadRequest());
  }

  @Test
  void endLeaveNoActiveLeaveTest() throws Exception {
    when(leaveLogService.endLeave(USER_ID)).thenThrow(new WorkException.NoActiveLeave(USER_ID));

    mockMvc.perform(post("/api/leave-log/end").header("Authorization", BEARER_TOKEN))
        .andExpect(status().is4xxClientError());
  }
}
