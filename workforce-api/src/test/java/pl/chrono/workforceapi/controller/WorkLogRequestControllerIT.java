package pl.chrono.workforceapi.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
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
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import pl.chrono.dbmanager.model.enums.WorkLogRequestStatus;
import pl.chrono.timemanager.dto.request.WorkLogCreateRequest;
import pl.chrono.timemanager.dto.request.WorkLogResolveRequest;
import pl.chrono.timemanager.dto.response.WorkLogRequestDto;
import pl.chrono.timemanager.exception.WorkLogRequestException;
import pl.chrono.timemanager.service.WorkLogRequestService;
import pl.chrono.usermanager.security.JwtService;
import pl.chrono.usermanager.service.AuthService;
import pl.chrono.workforceapi.TestApiConfig;

@WebMvcTest(WorkLogRequestController.class)
@Import(TestApiConfig.class)
@AutoConfigureMockMvc(addFilters = false)
class WorkLogRequestControllerIT {

  private static final String BEARER_TOKEN = "Bearer dummy.jwt.token";
  private static final Integer EMPLOYEE_ID = 1;
  private static final Integer MANAGER_ID = 2;
  private static final Long REQUEST_ID = 100L;
  private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
  @Autowired
  private MockMvc mockMvc;
  @MockitoBean
  private WorkLogRequestService requestService;
  @MockitoBean
  private AuthService authService;
  @MockitoBean
  private JwtService jwtService;
  @MockitoBean
  private UserDetailsService userDetailsService;

  @BeforeEach
  void setUp() {
    when(authService.extractUserIdFromToken(anyString())).thenReturn(EMPLOYEE_ID);
  }

  private WorkLogRequestDto sampleDto(WorkLogRequestStatus status) {
    return new WorkLogRequestDto(
        REQUEST_ID, EMPLOYEE_ID, "Jan Kowalski", MANAGER_ID, "Anna Nowak", status,
        LocalDateTime.now().minusHours(2), LocalDateTime.now().minusHours(1),
        "Zapomniałem zakończyć pracę", null, LocalDateTime.now(), null
    );
  }

  @Test
  void submitRequestCreatedTest() throws Exception {
    WorkLogCreateRequest request = new WorkLogCreateRequest(
        LocalDate.now(), LocalDateTime.now().minusHours(2), LocalDateTime.now().minusHours(1),
        "Zapomniałem zakończyć pracę"
    );
    doNothing().when(requestService)
        .submitRequest(eq(EMPLOYEE_ID), any(WorkLogCreateRequest.class));

    mockMvc.perform(post("/api/work-log-requests").header("Authorization", BEARER_TOKEN)
            .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated());
  }

  @Test
  void submitRequestWrongDateRange4xxTest() throws Exception {
    WorkLogCreateRequest request = new WorkLogCreateRequest(
        LocalDate.now(), LocalDateTime.now().minusHours(1),
        LocalDateTime.now().minusHours(2),
        "Test"
    );
    doThrow(new WorkLogRequestException.InvalidTimeRange()).when(requestService)
        .submitRequest(eq(EMPLOYEE_ID), any(WorkLogCreateRequest.class));

    mockMvc.perform(post("/api/work-log-requests").header("Authorization", BEARER_TOKEN)
            .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(request)))
        .andExpect(status().is4xxClientError());
  }

  @Test
  void getMyRequestsListTest() throws Exception {
    when(requestService.getEmployeeRequests(EMPLOYEE_ID)).thenReturn(
        List.of(sampleDto(WorkLogRequestStatus.PENDING)));

    mockMvc.perform(get("/api/work-log-requests/me").header("Authorization", BEARER_TOKEN))
        .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1))
        .andExpect(jsonPath("$[0].id").value(REQUEST_ID))
        .andExpect(jsonPath("$[0].status").value("PENDING"));
  }

  @Test
  void getMyRequestsEmptyListTest() throws Exception {
    when(requestService.getEmployeeRequests(EMPLOYEE_ID)).thenReturn(List.of());

    mockMvc.perform(get("/api/work-log-requests/me").header("Authorization", BEARER_TOKEN))
        .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));
  }

  @Test
  void getMyRequestsAllStatusesTest() throws Exception {
    when(requestService.getEmployeeRequests(EMPLOYEE_ID)).thenReturn(
        List.of(
            sampleDto(WorkLogRequestStatus.PENDING), sampleDto(WorkLogRequestStatus.APPROVED),
            sampleDto(WorkLogRequestStatus.REJECTED)
        ));

    mockMvc.perform(get("/api/work-log-requests/me").header("Authorization", BEARER_TOKEN))
        .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(3));
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void getPendingRequestsListTest() throws Exception {
    when(requestService.getPendingRequestsForReviewer(EMPLOYEE_ID)).thenReturn(
        List.of(sampleDto(WorkLogRequestStatus.PENDING)));

    mockMvc.perform(get("/api/work-log-requests/pending").header("Authorization", BEARER_TOKEN))
        .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1))
        .andExpect(jsonPath("$[0].status").value("PENDING"));
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void getPendingRequestsEmptyListTest() throws Exception {
    when(requestService.getPendingRequestsForReviewer(EMPLOYEE_ID)).thenReturn(List.of());

    mockMvc.perform(get("/api/work-log-requests/pending").header("Authorization", BEARER_TOKEN))
        .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void approveRequestOkTest() throws Exception {
    WorkLogResolveRequest resolve = new WorkLogResolveRequest("Wygląda dobrze");
    doNothing().when(requestService)
        .approveRequest(eq(REQUEST_ID), eq(EMPLOYEE_ID), any(WorkLogResolveRequest.class));

    mockMvc.perform(post("/api/work-log-requests/{id}/approve", REQUEST_ID).header(
            "Authorization",
            BEARER_TOKEN
        ).contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(resolve)))
        .andExpect(status().isOk());
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void approveRequestNoCommentTest() throws Exception {
    doNothing().when(requestService).approveRequest(eq(REQUEST_ID), eq(EMPLOYEE_ID), isNull());

    mockMvc.perform(post("/api/work-log-requests/{id}/approve", REQUEST_ID).header(
        "Authorization",
        BEARER_TOKEN
    )).andExpect(status().isOk());
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void approveRequestNotFound4xxTest() throws Exception {
    doThrow(new WorkLogRequestException.NotFound(REQUEST_ID)).when(requestService)
        .approveRequest(eq(999L), eq(EMPLOYEE_ID), any());

    mockMvc.perform(
            post("/api/work-log-requests/{id}/approve", 999L).header("Authorization", BEARER_TOKEN))
        .andExpect(status().is4xxClientError());
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void rejectRequestOkTest() throws Exception {
    WorkLogResolveRequest resolve = new WorkLogResolveRequest("Godziny się nie zgadzają");
    doNothing().when(requestService)
        .rejectRequest(eq(REQUEST_ID), eq(EMPLOYEE_ID), any(WorkLogResolveRequest.class));

    mockMvc.perform(
        post("/api/work-log-requests/{id}/reject", REQUEST_ID).header("Authorization", BEARER_TOKEN)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(resolve))).andExpect(status().isOk());
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void rejectRequestNoCommentTest() throws Exception {
    doNothing().when(requestService).rejectRequest(eq(REQUEST_ID), eq(EMPLOYEE_ID), isNull());

    mockMvc.perform(post("/api/work-log-requests/{id}/reject", REQUEST_ID).header(
        "Authorization",
        BEARER_TOKEN
    )).andExpect(status().isOk());
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void rejectRequestNotFound4xxTest() throws Exception {
    doThrow(new WorkLogRequestException.NotFound(REQUEST_ID)).when(requestService)
        .rejectRequest(eq(999L), eq(EMPLOYEE_ID), any());

    mockMvc.perform(
            post("/api/work-log-requests/{id}/reject", 999L).header("Authorization", BEARER_TOKEN))
        .andExpect(status().is4xxClientError());
  }
}
