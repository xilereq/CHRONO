package pl.chrono.workforceapi.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import pl.chrono.applicationmanager.api.I_UserApplication;
import pl.chrono.applicationmanager.dto.request.ChangeApplicationStatusRequest;
import pl.chrono.applicationmanager.dto.request.CreateApplicationRequest;
import pl.chrono.applicationmanager.dto.response.ApplicationResponse;
import pl.chrono.applicationmanager.exception.ApplicationManagerException;
import pl.chrono.usermanager.security.JwtService;
import pl.chrono.usermanager.service.AuthService;
import pl.chrono.workforceapi.TestApiConfig;

@WebMvcTest(ApplicationController.class)
@Import(TestApiConfig.class)
@AutoConfigureMockMvc(addFilters = false)
class ApplicationControllerIT {

  private static final String BEARER_TOKEN = "Bearer dummy.jwt.token";
  private static final Integer USER_ID = 1;
  private static final Integer TARGET_EMPLOYEE_ID = 2;

  private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

  @Autowired
  private MockMvc mockMvc;

  @MockitoBean
  private I_UserApplication applicationService;

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

  private ApplicationResponse sampleApplicationResponse(Long id, String status) {
    return new ApplicationResponse(
        id, "VACATION", status,
        LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 10),
        28800, "Wypoczynek", LocalDateTime.of(2026, 6, 1, 10, 0),
        null, null, USER_ID, "user@example.com", null, null
    );
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void createApplicationTest() throws Exception {
    String requestJson = "{\"type\":\"VACATION\",\"startDate\":\"2026-07-01\",\"endDate\":\"2026-07-10\"}";

    when(applicationService.createApplication(any(), eq(USER_ID)))
        .thenReturn(sampleApplicationResponse(1L, "PENDING"));

    mockMvc.perform(post("/api/applications/employee")
            .header("Authorization", BEARER_TOKEN)
            .contentType(MediaType.APPLICATION_JSON)
            .content(requestJson))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").value(1))
        .andExpect(jsonPath("$.type").value("VACATION"))
        .andExpect(jsonPath("$.status").value("PENDING"));
  }

  @Test
  void createApplicationBadRequestTest() throws Exception {
    mockMvc.perform(post("/api/applications/employee")
            .header("Authorization", BEARER_TOKEN)
            .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isBadRequest());
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void createApplicationForEmployeeTest() throws Exception {
    String requestJson = "{\"type\":\"SICK_LEAVE\"}";

    when(applicationService.createApplicationForEmployee(any(), eq(TARGET_EMPLOYEE_ID), eq(USER_ID)))
        .thenReturn(sampleApplicationResponse(2L, "APPROVED"));

    mockMvc.perform(post("/api/applications/employee/{targetEmployeeId}", TARGET_EMPLOYEE_ID)
            .header("Authorization", BEARER_TOKEN)
            .contentType(MediaType.APPLICATION_JSON)
            .content(requestJson))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").value(2));
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void getMyApplicationsTest() throws Exception {
    when(applicationService.getUserApplications(USER_ID, USER_ID))
        .thenReturn(List.of(sampleApplicationResponse(1L, "PENDING")));

    mockMvc.perform(get("/api/applications/my")
            .header("Authorization", BEARER_TOKEN))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(1))
        .andExpect(jsonPath("$[0].status").value("PENDING"));
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void getUserApplicationsTest() throws Exception {
    when(applicationService.getUserApplications(TARGET_EMPLOYEE_ID, USER_ID)).thenReturn(List.of());

    mockMvc.perform(get("/api/applications/employee/{employeeId}", TARGET_EMPLOYEE_ID)
            .header("Authorization", BEARER_TOKEN))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(0));
  }

  @Test
  @WithMockUser(roles = "MANAGER")
  void getPendingApplicationsForManagerTest() throws Exception {
    when(applicationService.getPendingApplicationsForManager(USER_ID)).thenReturn(List.of());

    mockMvc.perform(get("/api/applications/manager/pending")
            .header("Authorization", BEARER_TOKEN))
        .andExpect(status().isOk());
  }

  @Test
  @WithMockUser(roles = "MANAGER")
  void getAllApplicationsForManagerTest() throws Exception {
    when(applicationService.getAllApplicationsForManager(USER_ID)).thenReturn(List.of());

    mockMvc.perform(get("/api/applications/manager/all")
            .header("Authorization", BEARER_TOKEN))
        .andExpect(status().isOk());
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void getAllApplicationsTest() throws Exception {
    when(applicationService.getAllApplications()).thenReturn(List.of());

    mockMvc.perform(get("/api/applications/all")
            .header("Authorization", BEARER_TOKEN))
        .andExpect(status().isOk());
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void changeStatusOkTest() throws Exception {
    Long applicationId = 10L;
    String requestJson = "{\"newStatus\":\"APPROVED\",\"decisionReason\":\"Zgadzam się\"}";

    Authentication authentication = new UsernamePasswordAuthenticationToken(
        "admin", null, List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));

    when(applicationService.changeStatus(eq(applicationId), any(), eq(USER_ID), anyString(), eq(true)))
        .thenReturn(sampleApplicationResponse(applicationId, "APPROVED"));

    mockMvc.perform(patch("/api/applications/{applicationId}/status", applicationId)
            .header("Authorization", BEARER_TOKEN)
            .principal(authentication)
            .contentType(MediaType.APPLICATION_JSON)
            .content(requestJson))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("APPROVED"));
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void changeStatusApplicationNotFoundTest() throws Exception {
    Long invalidApplicationId = 999L;
    String requestJson = "{\"newStatus\":\"REJECTED\"}";

    Authentication authentication = new UsernamePasswordAuthenticationToken(
        "manager", null, List.of(new SimpleGrantedAuthority("ROLE_MANAGER")));

    when(applicationService.changeStatus(eq(invalidApplicationId), any(), eq(USER_ID), any(), eq(false)))
        .thenThrow(new ApplicationManagerException("App not found", "APP_NOT_FOUND", HttpStatus.NOT_FOUND));

    mockMvc.perform(patch("/api/applications/{applicationId}/status", invalidApplicationId)
            .header("Authorization", BEARER_TOKEN)
            .principal(authentication)
            .contentType(MediaType.APPLICATION_JSON)
            .content(requestJson))
        .andExpect(status().isNotFound());
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void cancelOwnApplicationOkTest() throws Exception {
    Long applicationId = 100L;
    when(applicationService.cancelApplication(applicationId, USER_ID))
        .thenReturn(sampleApplicationResponse(applicationId, "CANCELLED"));

    mockMvc.perform(patch("/api/applications/{applicationId}/cancel", applicationId)
            .header("Authorization", BEARER_TOKEN))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("CANCELLED"));
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void cancelOwnApplicationForbiddenTest() throws Exception {
    Long applicationId = 100L;
    when(applicationService.cancelApplication(applicationId, USER_ID))
        .thenThrow(new ApplicationManagerException("Cannot cancel processed application", "APP_PROCESSED", HttpStatus.FORBIDDEN));

    mockMvc.perform(patch("/api/applications/{applicationId}/cancel", applicationId)
            .header("Authorization", BEARER_TOKEN))
        .andExpect(status().isForbidden());
  }
}
