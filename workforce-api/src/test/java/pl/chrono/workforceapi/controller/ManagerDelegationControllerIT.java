package pl.chrono.workforceapi.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import pl.chrono.usermanager.api.I_ManagerDelegationService;
import pl.chrono.usermanager.dto.response.ManagerDelegationDto;
import pl.chrono.usermanager.exception.ManagerDelegationException;
import pl.chrono.usermanager.security.JwtService;
import pl.chrono.usermanager.service.AuthService;
import pl.chrono.workforceapi.TestApiConfig;

@WebMvcTest(ManagerDelegationController.class)
@Import(TestApiConfig.class)
@AutoConfigureMockMvc(addFilters = false)
class ManagerDelegationControllerIT {

  private static final String BEARER_TOKEN = "Bearer dummy.jwt.token";
  private static final Integer USER_ID = 1;
  private static final String VALID_BODY =
      "{\"deputyId\":2,\"startDate\":\"2026-11-02\",\"endDate\":\"2026-11-10\",\"reason\":\"Urlop\"}";

  @Autowired
  private MockMvc mockMvc;

  @MockitoBean
  private I_ManagerDelegationService delegationService;

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

  private static ManagerDelegationDto sampleDto(String status) {
    return new ManagerDelegationDto(10L, USER_ID, "Jan Kowalski", 2, "Anna Nowak",
        LocalDate.of(2026, 11, 2), LocalDate.of(2026, 11, 10), "Urlop", status,
        USER_ID, LocalDateTime.of(2026, 11, 1, 12, 0), null);
  }

  @Test
  @WithMockUser(roles = "MANAGER")
  void createDelegation_ReturnsCreated() throws Exception {
    when(delegationService.createDelegation(any(), eq(USER_ID))).thenReturn(sampleDto("SCHEDULED"));

    mockMvc.perform(post("/api/delegations")
            .header("Authorization", BEARER_TOKEN)
            .contentType(MediaType.APPLICATION_JSON)
            .content(VALID_BODY))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").value(10))
        .andExpect(jsonPath("$.deputyId").value(2))
        .andExpect(jsonPath("$.status").value("SCHEDULED"));
  }

  @Test
  @WithMockUser(roles = "EMPLOYEE")
  void createDelegation_ForbiddenForEmployee() throws Exception {
    mockMvc.perform(post("/api/delegations")
            .header("Authorization", BEARER_TOKEN)
            .contentType(MediaType.APPLICATION_JSON)
            .content(VALID_BODY))
        .andExpect(status().isForbidden());

    verify(delegationService, never()).createDelegation(any(), any());
  }

  @Test
  @WithMockUser(roles = "MANAGER")
  void createDelegation_ValidationErrorWhenDeputyMissing() throws Exception {
    mockMvc.perform(post("/api/delegations")
            .header("Authorization", BEARER_TOKEN)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"startDate\":\"2026-11-02\",\"endDate\":\"2026-11-10\"}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
  }

  @Test
  @WithMockUser(roles = "MANAGER")
  void createDelegation_MapsServiceExceptionToItsStatus() throws Exception {
    when(delegationService.createDelegation(any(), eq(USER_ID)))
        .thenThrow(new ManagerDelegationException.Overlapping());

    mockMvc.perform(post("/api/delegations")
            .header("Authorization", BEARER_TOKEN)
            .contentType(MediaType.APPLICATION_JSON)
            .content(VALID_BODY))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("DELEGATION_OVERLAPPING"));
  }

  @Test
  @WithMockUser(roles = "MANAGER")
  void revokeDelegation_ReturnsRevoked() throws Exception {
    when(delegationService.revokeDelegation(10L, USER_ID)).thenReturn(sampleDto("REVOKED"));

    mockMvc.perform(patch("/api/delegations/10/revoke").header("Authorization", BEARER_TOKEN))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("REVOKED"));
  }

  @Test
  @WithMockUser(roles = "MANAGER")
  void getGivenDelegations_ReturnsList() throws Exception {
    when(delegationService.getDelegationsGivenBy(USER_ID)).thenReturn(List.of(sampleDto("ACTIVE")));

    mockMvc.perform(get("/api/delegations/given").header("Authorization", BEARER_TOKEN))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].status").value("ACTIVE"));
  }

  @Test
  @WithMockUser(roles = "EMPLOYEE")
  void getReceivedDelegations_AvailableForEmployee() throws Exception {
    when(delegationService.getDelegationsReceivedBy(USER_ID)).thenReturn(List.of(sampleDto("ACTIVE")));

    mockMvc.perform(get("/api/delegations/received").header("Authorization", BEARER_TOKEN))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].deputyName").value("Anna Nowak"));
  }

  @Test
  @WithMockUser(roles = "MANAGER")
  void getAllDelegations_ForbiddenForManager() throws Exception {
    mockMvc.perform(get("/api/delegations").header("Authorization", BEARER_TOKEN))
        .andExpect(status().isForbidden());
  }

  @Test
  @WithMockUser(roles = "HR")
  void getAllDelegations_AllowedForHr() throws Exception {
    when(delegationService.getAllDelegations()).thenReturn(List.of(sampleDto("ACTIVE")));

    mockMvc.perform(get("/api/delegations").header("Authorization", BEARER_TOKEN))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].id").value(10));
  }
}
