package pl.chrono.workforceapi.controller;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Stream;
import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import pl.chrono.dbmanager.model.enums.ReportPeriod;
import pl.chrono.dbmanager.model.enums.ScheduleFrequency;
import pl.chrono.dbmanager.model.enums.ScheduledReportType;
import pl.chrono.reportmanager.api.I_ScheduledReportService;
import pl.chrono.reportmanager.dto.ReportFormat;
import pl.chrono.reportmanager.dto.ScheduledReportRequestDto;
import pl.chrono.reportmanager.dto.ScheduledReportResponseDto;
import pl.chrono.reportmanager.exception.ErrorCodes;
import pl.chrono.reportmanager.exception.ReportException;
import pl.chrono.reportmanager.exception.ReportScheduleException;
import pl.chrono.reportmanager.exception.ReportScheduleSecurityException;
import pl.chrono.reportmanager.exception.ReportUserException;
import pl.chrono.usermanager.security.JwtService;
import pl.chrono.usermanager.service.AuthService;
import pl.chrono.workforceapi.TestApiConfig;

@WebMvcTest(ScheduledReportController.class)
@Import(TestApiConfig.class)
@AutoConfigureMockMvc(addFilters = false)
class ScheduledReportControllerIT {

  private static final String BEARER_TOKEN = "Bearer dummy.jwt.token";
  private static final Integer USER_ID = 1;

  private static final String VALID_REQUEST_JSON = """
      {
        "name": "Raport dzienny",
        "reportType": "EMPLOYEE_TIMESHEET",
        "reportFormat": "PDF",
        "frequency": "DAILY",
        "executionTime": "07:00:00",
        "reportPeriod": "PREVIOUS_DAY",
        "targetUserIds": [1],
        "recipientEmails": ["jan@chrono.pl"]
      }
      """;

  @Autowired
  private MockMvc mockMvc;

  @MockitoBean
  private I_ScheduledReportService scheduledReportService;

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

  private ScheduledReportResponseDto createSampleResponse(Long scheduleId, boolean active) {
    return new ScheduledReportResponseDto(
        scheduleId,
        "Raport dzienny",
        USER_ID,
        "Jan Kowalski",
        ScheduledReportType.EMPLOYEE_TIMESHEET,
        ReportFormat.PDF,
        ScheduleFrequency.DAILY,
        null,
        null,
        LocalTime.of(7, 0),
        ReportPeriod.PREVIOUS_DAY,
        null,
        null,
        List.of(USER_ID),
        List.of("jan@chrono.pl"),
        active,
        LocalDateTime.of(2026, 5, 2, 7, 0),
        null,
        null,
        0
    );
  }

  @Test
  @WithMockUser(roles = "EMPLOYEE")
  void createScheduleCreatedTest() throws Exception {
    ScheduledReportResponseDto responseDto = createSampleResponse(10L, true);
    when(scheduledReportService.createSchedule(eq(USER_ID), any(ScheduledReportRequestDto.class)))
        .thenReturn(responseDto);

    mockMvc.perform(post("/api/reports/schedules")
            .header("Authorization", BEARER_TOKEN)
            .contentType(MediaType.APPLICATION_JSON)
            .content(VALID_REQUEST_JSON))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.scheduleId").value(10))
        .andExpect(jsonPath("$.name").value("Raport dzienny"))
        .andExpect(jsonPath("$.isActive").value(true));
  }

  @Test
  @WithMockUser(roles = "EMPLOYEE")
  void getSchedulesOkTest() throws Exception {
    ScheduledReportResponseDto responseDto = createSampleResponse(10L, true);
    when(scheduledReportService.getSchedules(USER_ID)).thenReturn(List.of(responseDto));

    mockMvc.perform(get("/api/reports/schedules")
            .header("Authorization", BEARER_TOKEN))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].scheduleId").value(10))
        .andExpect(jsonPath("$[0].creatorFullName").value("Jan Kowalski"));
  }

  @Test
  @WithMockUser(roles = "EMPLOYEE")
  void getScheduleByIdOkTest() throws Exception {
    ScheduledReportResponseDto responseDto = createSampleResponse(10L, true);
    when(scheduledReportService.getScheduleById(USER_ID, 10L)).thenReturn(responseDto);

    mockMvc.perform(get("/api/reports/schedules/{scheduleId}", 10L)
            .header("Authorization", BEARER_TOKEN))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.scheduleId").value(10))
        .andExpect(jsonPath("$.reportFormat").value("PDF"));
  }

  @Test
  @WithMockUser(roles = "EMPLOYEE")
  void updateScheduleOkTest() throws Exception {
    ScheduledReportResponseDto responseDto = createSampleResponse(10L, true);
    when(scheduledReportService.updateSchedule(
        eq(USER_ID), eq(10L), any(ScheduledReportRequestDto.class)))
        .thenReturn(responseDto);

    mockMvc.perform(put("/api/reports/schedules/{scheduleId}", 10L)
            .header("Authorization", BEARER_TOKEN)
            .contentType(MediaType.APPLICATION_JSON)
            .content(VALID_REQUEST_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.scheduleId").value(10));
  }

  @Test
  @WithMockUser(roles = "EMPLOYEE")
  void toggleScheduleOkTest() throws Exception {
    ScheduledReportResponseDto responseDto = createSampleResponse(10L, false);
    when(scheduledReportService.toggleSchedule(USER_ID, 10L)).thenReturn(responseDto);

    mockMvc.perform(patch("/api/reports/schedules/{scheduleId}/toggle", 10L)
            .header("Authorization", BEARER_TOKEN))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.scheduleId").value(10))
        .andExpect(jsonPath("$.isActive").value(false));
  }

  @Test
  @WithMockUser(roles = "EMPLOYEE")
  void triggerScheduleNowNoContentTest() throws Exception {
    doNothing().when(scheduledReportService).triggerScheduleNow(USER_ID, 10L);

    mockMvc.perform(post("/api/reports/schedules/{scheduleId}/trigger", 10L)
            .header("Authorization", BEARER_TOKEN))
        .andExpect(status().isNoContent());

    verify(scheduledReportService).triggerScheduleNow(USER_ID, 10L);
  }

  @Test
  @WithMockUser(roles = "EMPLOYEE")
  void deleteScheduleNoContentTest() throws Exception {
    doNothing().when(scheduledReportService).deleteSchedule(USER_ID, 10L);

    mockMvc.perform(delete("/api/reports/schedules/{scheduleId}", 10L)
            .header("Authorization", BEARER_TOKEN))
        .andExpect(status().isNoContent());

    verify(scheduledReportService).deleteSchedule(USER_ID, 10L);
  }

  @Test
  @WithMockUser(roles = "EMPLOYEE")
  void createScheduleValidationBadRequestTest() throws Exception {
    String invalidJson = """
        {
          "name": "",
          "reportType": null,
          "reportFormat": null,
          "frequency": null,
          "dayOfWeek": 9,
          "reportPeriod": null,
          "targetUserIds": [],
          "recipientEmails": []
        }
        """;

    mockMvc.perform(post("/api/reports/schedules")
            .header("Authorization", BEARER_TOKEN)
            .contentType(MediaType.APPLICATION_JSON)
            .content(invalidJson))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
        .andExpect(jsonPath("$.message").isNotEmpty())
        .andExpect(jsonPath("$.timestamp").isNotEmpty());
  }

  private static Stream<Arguments> scheduleExceptionsProvider() {
    return Stream.of(
        Arguments.of(
            new ReportScheduleException.NotFound(99L),
            404,
            ErrorCodes.SCHEDULE_NOT_FOUND
        ),
        Arguments.of(
            new ReportScheduleException.UsersNotFound(List.of(50, 60)),
            404,
            ErrorCodes.TARGET_USERS_NOT_FOUND
        ),
        Arguments.of(
            new ReportUserException.NotFound(USER_ID),
            404,
            ErrorCodes.USER_NOT_FOUND
        ),
        Arguments.of(
            new ReportScheduleException.MissingDayOfWeek(),
            400,
            ErrorCodes.MISSING_DAY_OF_WEEK
        ),
        Arguments.of(
            new ReportScheduleException.MissingDayOfMonth(),
            400,
            ErrorCodes.MISSING_DAY_OF_MONTH
        ),
        Arguments.of(
            new ReportScheduleException.InvalidCustomDateRange(),
            400,
            ErrorCodes.INVALID_CUSTOM_DATE_RANGE
        ),
        Arguments.of(
            new ReportScheduleSecurityException.UnauthorizedTeamReport(),
            403,
            ErrorCodes.UNAUTHORIZED_TEAM_REPORT
        ),
        Arguments.of(
            new ReportScheduleSecurityException.UnauthorizedTargetUser(),
            403,
            ErrorCodes.UNAUTHORIZED_TARGET_USER
        ),
        Arguments.of(
            new ReportScheduleSecurityException.UnauthorizedRecipientEmail(),
            403,
            ErrorCodes.UNAUTHORIZED_RECIPIENT_EMAIL
        ),
        Arguments.of(
            new ReportScheduleSecurityException.UnauthorizedManagerTargetUsers(),
            403,
            ErrorCodes.UNAUTHORIZED_MANAGER_TARGET_USERS
        ),
        Arguments.of(
            new ReportScheduleSecurityException.UnauthorizedScheduleAccess(),
            403,
            ErrorCodes.UNAUTHORIZED_SCHEDULE_ACCESS
        )
    );
  }

  @ParameterizedTest
  @MethodSource("scheduleExceptionsProvider")
  @WithMockUser(roles = "EMPLOYEE")
  void createScheduleMapsReportExceptionsToExpectedHttpStatusAndBodyTest(
      ReportException exception, int expectedStatus, String expectedCode) throws Exception {
    doThrow(exception)
        .when(scheduledReportService)
        .createSchedule(eq(USER_ID), any(ScheduledReportRequestDto.class));

    mockMvc.perform(post("/api/reports/schedules")
            .header("Authorization", BEARER_TOKEN)
            .contentType(MediaType.APPLICATION_JSON)
            .content(VALID_REQUEST_JSON))
        .andExpect(status().is(expectedStatus))
        .andExpect(jsonPath("$.code").value(expectedCode))
        .andExpect(jsonPath("$.message").value(exception.getMessage()))
        .andExpect(jsonPath("$.timestamp").isNotEmpty());
  }

  @Test
  @WithMockUser(roles = "EMPLOYEE")
  void createScheduleDeserializesRequestBodyTest() throws Exception {
    when(scheduledReportService.createSchedule(eq(USER_ID), any(ScheduledReportRequestDto.class)))
        .thenReturn(createSampleResponse(10L, true));

    mockMvc.perform(post("/api/reports/schedules")
            .header("Authorization", BEARER_TOKEN)
            .contentType(MediaType.APPLICATION_JSON)
            .content(VALID_REQUEST_JSON))
        .andExpect(status().isCreated());

    ArgumentCaptor<ScheduledReportRequestDto> captor =
        ArgumentCaptor.forClass(ScheduledReportRequestDto.class);
    verify(scheduledReportService).createSchedule(eq(USER_ID), captor.capture());

    ScheduledReportRequestDto sent = captor.getValue();
    assertThat(sent.name()).isEqualTo("Raport dzienny");
    assertThat(sent.reportType()).isEqualTo(ScheduledReportType.EMPLOYEE_TIMESHEET);
    assertThat(sent.reportFormat()).isEqualTo(ReportFormat.PDF);
    assertThat(sent.frequency()).isEqualTo(ScheduleFrequency.DAILY);
    assertThat(sent.executionTime()).isEqualTo(LocalTime.of(7, 0));
    assertThat(sent.reportPeriod()).isEqualTo(ReportPeriod.PREVIOUS_DAY);
    assertThat(sent.targetUserIds()).containsExactly(1);
    assertThat(sent.recipientEmails()).containsExactly("jan@chrono.pl");
  }

  @ParameterizedTest
  @ValueSource(strings = {
      "\"dayOfWeek\": 0", "\"dayOfWeek\": 8", "\"dayOfMonth\": 0", "\"dayOfMonth\": 29"})
  @WithMockUser(roles = "EMPLOYEE")
  void createScheduleDayOutOfRangeBadRequestTest(String outOfRangeField) throws Exception {
    String body = VALID_REQUEST_JSON.replace(
        "\"frequency\": \"DAILY\"", "\"frequency\": \"DAILY\", " + outOfRangeField);

    mockMvc.perform(post("/api/reports/schedules")
            .header("Authorization", BEARER_TOKEN)
            .contentType(MediaType.APPLICATION_JSON)
            .content(body))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

    verifyNoInteractions(scheduledReportService);
  }

  @Test
  @WithMockUser(roles = "EMPLOYEE")
  void createScheduleUnknownEnumValueTest() throws Exception {
    String body = VALID_REQUEST_JSON.replace("EMPLOYEE_TIMESHEET", "NOT_A_REPORT");

    mockMvc.perform(post("/api/reports/schedules")
            .header("Authorization", BEARER_TOKEN)
            .contentType(MediaType.APPLICATION_JSON)
            .content(body))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("MALFORMED_JSON_REQUEST"));

    verifyNoInteractions(scheduledReportService);
  }

  @Test
  @WithMockUser(roles = "EMPLOYEE")
  void getSchedulesWithoutAuthorizationHeaderTest() throws Exception {
    mockMvc.perform(get("/api/reports/schedules"))
        .andExpect(status().isBadRequest());

    verifyNoInteractions(scheduledReportService);
  }
}
