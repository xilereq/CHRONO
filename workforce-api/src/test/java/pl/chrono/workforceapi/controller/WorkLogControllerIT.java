package pl.chrono.workforceapi.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import pl.chrono.dbmanager.model.enums.WorkLocationType;
import pl.chrono.timemanager.dto.request.StartWorkRequest;
import pl.chrono.timemanager.dto.response.WorkLogDto;
import pl.chrono.timemanager.dto.response.WorkStats;
import pl.chrono.timemanager.exception.WorkException;
import pl.chrono.timemanager.service.WorkLogService;
import pl.chrono.usermanager.security.JwtService;
import pl.chrono.usermanager.service.AuthService;
import pl.chrono.workforceapi.TestApiConfig;

@WebMvcTest(WorkLogController.class)
@Import(TestApiConfig.class)
@AutoConfigureMockMvc(addFilters = false)
class WorkLogControllerIT {

  private static final String BEARER_TOKEN = "Bearer dummy.jwt.token";
  private static final Integer USER_ID = 1;
  private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
  @Autowired
  private MockMvc mockMvc;
  @MockitoBean
  private WorkLogService workLogService;
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

  private WorkLogDto sampleWorkLogDto(LocalDateTime start, LocalDateTime end,
                                      WorkLocationType type) {
    WorkLogDto dto = new WorkLogDto();
    dto.setWorkLogId(1);
    dto.setUserId(USER_ID);
    dto.setStartTime(start);
    dto.setEndTime(end);
    dto.setLocationType(type);
    return dto;
  }

  @Test
  void getWorkLogsByDateListTest() throws Exception {
    LocalDate date = LocalDate.now();
    WorkLogDto dto = sampleWorkLogDto(
        date.atTime(8, 0), date.atTime(16, 0), WorkLocationType.OFFICE);
    when(workLogService.getWorkLogsByDate(USER_ID, date)).thenReturn(List.of(dto));

    mockMvc.perform(get("/api/work-log/by-date").header("Authorization", BEARER_TOKEN)
            .param("date", date.toString())).andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(1))
        .andExpect(jsonPath("$[0].userId").value(USER_ID))
        .andExpect(jsonPath("$[0].locationType").value("OFFICE"));
  }

  @Test
  void getWorkLogsByDateEmptyDay4xxTest() throws Exception {
    LocalDate date = LocalDate.now();
    when(workLogService.getWorkLogsByDate(USER_ID, date)).thenReturn(List.of());

    mockMvc.perform(get("/api/work-log/by-date").header("Authorization", BEARER_TOKEN)
            .param("date", date.toString())).andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(0));
  }

  @Test
  void getWorkLogsByDateWrongDate4xxTest() throws Exception {
    mockMvc.perform(get("/api/work-log/by-date").header("Authorization", BEARER_TOKEN))
        .andExpect(status().is4xxClientError());
  }

  @Test
  void isWorkingTrueTest() throws Exception {
    when(workLogService.isUserWorking(USER_ID)).thenReturn(true);

    mockMvc.perform(get("/api/work-log/status").header("Authorization", BEARER_TOKEN))
        .andExpect(status().isOk()).andExpect(jsonPath("$").value(true));
  }

  @Test
  void isWorkingFalseTest() throws Exception {
    when(workLogService.isUserWorking(USER_ID)).thenReturn(false);

    mockMvc.perform(get("/api/work-log/status").header("Authorization", BEARER_TOKEN))
        .andExpect(status().isOk()).andExpect(jsonPath("$").value(false));
  }

  @Test
  void startWorkOfficeCreatedTest() throws Exception {
    StartWorkRequest request = new StartWorkRequest(WorkLocationType.OFFICE);
    WorkLogDto dto = sampleWorkLogDto(LocalDateTime.now(), null, WorkLocationType.OFFICE);
    when(workLogService.startWork(USER_ID, WorkLocationType.OFFICE)).thenReturn(dto);

    mockMvc.perform(post("/api/work-log/start").header("Authorization", BEARER_TOKEN)
            .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated()).andExpect(jsonPath("$.userId").value(USER_ID))
        .andExpect(jsonPath("$.locationType").value("OFFICE"))
        .andExpect(jsonPath("$.endTime").isEmpty());
  }

  @Test
  void startWorkHOCreatedTest() throws Exception {
    StartWorkRequest request = new StartWorkRequest(WorkLocationType.HOME_OFFICE);
    WorkLogDto dto = sampleWorkLogDto(LocalDateTime.now(), null, WorkLocationType.HOME_OFFICE);
    when(workLogService.startWork(USER_ID, WorkLocationType.HOME_OFFICE)).thenReturn(dto);

    mockMvc.perform(post("/api/work-log/start").header("Authorization", BEARER_TOKEN)
            .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated()).andExpect(jsonPath("$.locationType").value("HOME_OFFICE"));
  }

  @Test
  void startWorkCreatedTest() throws Exception {
    WorkLogDto dto = sampleWorkLogDto(LocalDateTime.now(), null, WorkLocationType.OFFICE);
    when(workLogService.startWork(eq(USER_ID), isNull())).thenReturn(dto);

    mockMvc.perform(post("/api/work-log/start").header("Authorization", BEARER_TOKEN))
        .andExpect(status().isCreated());
  }

  @Test
  void startWorkAlreadyWorking4xxTest() throws Exception {
    when(workLogService.startWork(USER_ID, null)).thenThrow(new WorkException.WorkStarted(USER_ID));

    mockMvc.perform(post("/api/work-log/start").header("Authorization", BEARER_TOKEN))
        .andExpect(status().is4xxClientError());
  }

  @Test
  void startWorkAlreadyWorked4xxTest() throws Exception {
    when(workLogService.startWork(USER_ID, null)).thenThrow(new WorkException.AlreadyWorkedToday());

    mockMvc.perform(post("/api/work-log/start").header("Authorization", BEARER_TOKEN))
        .andExpect(status().is4xxClientError());
  }

  @Test
  void startWorkNoHOApplication403Test() throws Exception {
    StartWorkRequest request = new StartWorkRequest(WorkLocationType.HOME_OFFICE);
    when(workLogService.startWork(USER_ID, WorkLocationType.HOME_OFFICE)).thenThrow(
        new WorkException.NoHomeOfficeApplication());

    mockMvc.perform(post("/api/work-log/start").header("Authorization", BEARER_TOKEN)
            .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isForbidden());
  }

  @Test
  void endWorkOkTest() throws Exception {
    WorkLogDto dto = sampleWorkLogDto(
        LocalDateTime.now().minusHours(8), LocalDateTime.now(), WorkLocationType.OFFICE);
    when(workLogService.endWork(USER_ID)).thenReturn(dto);

    mockMvc.perform(post("/api/work-log/end").header("Authorization", BEARER_TOKEN))
        .andExpect(status().isOk()).andExpect(jsonPath("$.endTime").isNotEmpty());
  }

  @Test
  void endWorkNotWorking4xxTest() throws Exception {
    when(workLogService.endWork(USER_ID)).thenThrow(new WorkException.NoActiveWork(USER_ID));

    mockMvc.perform(post("/api/work-log/end").header("Authorization", BEARER_TOKEN))
        .andExpect(status().is4xxClientError());
  }

  @Test
  void endWorkInsufficientWorkTimeTest() throws Exception {
    when(workLogService.endWork(USER_ID)).thenThrow(
        new WorkException.InsufficientWorkTime("06:00:00"));

    mockMvc.perform(post("/api/work-log/end").header("Authorization", BEARER_TOKEN))
        .andExpect(status().is4xxClientError());
  }

  @Test
  void endWorkOnLeave4xxTest() throws Exception {
    when(workLogService.endWork(USER_ID)).thenThrow(new WorkException.LeaveStarted(USER_ID));

    mockMvc.perform(post("/api/work-log/end").header("Authorization", BEARER_TOKEN))
        .andExpect(status().is4xxClientError());
  }

  @Test
  void getActiveDurationTest() throws Exception {
    when(workLogService.getActiveWorkTime(USER_ID)).thenReturn("04:30:00");

    mockMvc.perform(get("/api/work-log/active-time").header("Authorization", BEARER_TOKEN))
        .andExpect(status().isOk()).andExpect(jsonPath("$").value("04:30:00"));
  }

  @Test
  void getActiveDurationNotWorkingTest() throws Exception {
    when(workLogService.getActiveWorkTime(USER_ID)).thenReturn("00:00:00");

    mockMvc.perform(get("/api/work-log/active-time").header("Authorization", BEARER_TOKEN))
        .andExpect(status().isOk()).andExpect(jsonPath("$").value("00:00:00"));
  }

  @Test
  void getMonthlyStatsTest() throws Exception {
    WorkStats stats = new WorkStats(120, 40, 160, 7200);
    when(workLogService.getMonthlyStats(USER_ID)).thenReturn(stats);

    mockMvc.perform(get("/api/work-log/monthly-stats").header("Authorization", BEARER_TOKEN))
        .andExpect(status().isOk()).andExpect(jsonPath("$.totalWorkTime").value(120))
        .andExpect(jsonPath("$.remainingWorkTime").value(40))
        .andExpect(jsonPath("$.totalMonthlyHours").value(160))
        .andExpect(jsonPath("$.overtimeSeconds").value(7200));
  }

  @Test
  void getTodayTargetTest() throws Exception {
    when(workLogService.getTodayTarget(USER_ID)).thenReturn("08:00:00");

    mockMvc.perform(get("/api/work-log/today-target").header("Authorization", BEARER_TOKEN))
        .andExpect(status().isOk()).andExpect(jsonPath("$").value("08:00:00"));
  }

  @Test
  void cancelWorkOkTest() throws Exception {
    doNothing().when(workLogService).cancelWork(USER_ID);

    mockMvc.perform(post("/api/work-log/cancel").header("Authorization", BEARER_TOKEN))
        .andExpect(status().isOk());
  }

  @Test
  void cancelWorkNoActiveWork4xxTest() throws Exception {
    doThrow(new WorkException.NoActiveWork(USER_ID)).when(workLogService).cancelWork(USER_ID);

    mockMvc.perform(post("/api/work-log/cancel").header("Authorization", BEARER_TOKEN))
        .andExpect(status().is4xxClientError());
  }

  @Test
  void cancelWorkCancellationExpired4xxTest() throws Exception {
    doThrow(new WorkException.CancellationExpired()).when(workLogService).cancelWork(USER_ID);

    mockMvc.perform(post("/api/work-log/cancel").header("Authorization", BEARER_TOKEN))
        .andExpect(status().is4xxClientError());
  }
}
