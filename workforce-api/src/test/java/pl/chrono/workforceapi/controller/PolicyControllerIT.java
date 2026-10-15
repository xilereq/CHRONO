package pl.chrono.workforceapi.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import pl.chrono.timemanager.dto.request.AssignHolidayRequest;
import pl.chrono.timemanager.dto.request.AssignPolicyRequest;
import pl.chrono.timemanager.dto.request.CreatePolicyRequest;
import pl.chrono.timemanager.dto.request.MassAssignHolidayRequest;
import pl.chrono.timemanager.dto.request.MassAssignPolicyRequest;
import pl.chrono.timemanager.dto.response.HolidayDto;
import pl.chrono.timemanager.dto.response.PolicyDto;
import pl.chrono.timemanager.exception.PolicyException;
import pl.chrono.timemanager.service.PolicyConfigurationService;
import pl.chrono.timemanager.service.PolicyManagementService;
import pl.chrono.usermanager.security.JwtService;
import pl.chrono.usermanager.service.AuthService;
import pl.chrono.workforceapi.TestApiConfig;

@WebMvcTest(PolicyController.class)
@Import(TestApiConfig.class)
@AutoConfigureMockMvc(addFilters = false)
class PolicyControllerIT {

  private static final String BEARER_TOKEN = "Bearer dummy.jwt.token";
  private static final Integer USER_ID = 1;
  private static final Integer POLICY_ID = 10;
  private static final Integer HOLIDAY_ID = 5;
  private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
  @Autowired
  private MockMvc mockMvc;
  @MockitoBean
  private PolicyConfigurationService configService;
  @MockitoBean
  private PolicyManagementService managementService;
  @MockitoBean
  private AuthService authService;
  @MockitoBean
  private JwtService jwtService;
  @MockitoBean
  private UserDetailsService userDetailsService;

  private PolicyDto samplePolicyDto() {
    return new PolicyDto(
        POLICY_ID, "Standard", 8, 12, 48, 0,
        Set.of(
            DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY,
            DayOfWeek.FRIDAY
        )
    );
  }

  private HolidayDto sampleHolidayDto() {
    return new HolidayDto(HOLIDAY_ID, "Nowy Rok", LocalDate.of(2025, 1, 1));
  }

  @BeforeEach
  void setUp() {
    when(authService.extractUserIdFromToken(anyString())).thenReturn(USER_ID);
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void createPolicyOkTest() throws Exception {
    CreatePolicyRequest request = new CreatePolicyRequest(
        "Standard", 8, 12, 48, 0,
        List.of("MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY")
    );
    when(configService.createPolicy(any(CreatePolicyRequest.class))).thenReturn(samplePolicyDto());

    mockMvc.perform(post("/api/policies").header("Authorization", BEARER_TOKEN)
            .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(POLICY_ID))
        .andExpect(jsonPath("$.name").value("Standard"))
        .andExpect(jsonPath("$.dailyHours").value(8));
  }

  @Test
  void createPolicyNoNameBadRequestTest() throws Exception {
    CreatePolicyRequest request = new CreatePolicyRequest("", 8, 12, 48, 0, List.of("MONDAY"));

    mockMvc.perform(post("/api/policies").header("Authorization", BEARER_TOKEN)
            .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest());
  }

  @Test
  void createPolicyNegativeHoursBadRequestTest() throws Exception {
    CreatePolicyRequest request = new CreatePolicyRequest(
        "Standard", -8, 12, 48, 0, List.of("MONDAY"));

    mockMvc.perform(post("/api/policies").header("Authorization", BEARER_TOKEN)
            .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest());
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void getAllPoliciesTest() throws Exception {
    when(configService.getAllPolicies()).thenReturn(List.of(samplePolicyDto(), samplePolicyDto()));

    mockMvc.perform(get("/api/policies").header("Authorization", BEARER_TOKEN))
        .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(2));
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void getAllPoliciesEmptyListTest() throws Exception {
    when(configService.getAllPolicies()).thenReturn(List.of());

    mockMvc.perform(get("/api/policies").header("Authorization", BEARER_TOKEN))
        .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void getPolicyByIdTesty() throws Exception {
    when(configService.getPolicyById(POLICY_ID)).thenReturn(samplePolicyDto());

    mockMvc.perform(
            get("/api/policies/{policyId}", POLICY_ID).header("Authorization", BEARER_TOKEN))
        .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(POLICY_ID))
        .andExpect(jsonPath("$.name").value("Standard"));
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void getPolicyByIdNotFound4xxTest() throws Exception {
    when(configService.getPolicyById(999)).thenThrow(new PolicyException.NotFound(999));

    mockMvc.perform(get("/api/policies/{policyId}", 999).header("Authorization", BEARER_TOKEN))
        .andExpect(status().is4xxClientError());
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void deletePolicyTest() throws Exception {
    doNothing().when(configService).deletePolicy(POLICY_ID);

    mockMvc.perform(
            delete("/api/policies/{policyId}", POLICY_ID).header("Authorization", BEARER_TOKEN))
        .andExpect(status().isNoContent());
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void deletePolicyNotFound4xxTest() throws Exception {
    doThrow(new PolicyException.NotFound(999)).when(configService).deletePolicy(999);

    mockMvc.perform(delete("/api/policies/{policyId}", 999).header("Authorization", BEARER_TOKEN))
        .andExpect(status().is4xxClientError());
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void assignPolicyTest() throws Exception {
    AssignPolicyRequest request = new AssignPolicyRequest(POLICY_ID, LocalDate.now());
    doNothing().when(managementService)
        .assignPolicyToUser(eq(USER_ID), any(AssignPolicyRequest.class));

    mockMvc.perform(
        post("/api/policies/{userId}/assign", USER_ID).header("Authorization", BEARER_TOKEN)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request))).andExpect(status().isNoContent());
  }

  @Test
  void assignPolicyBadRequestTest() throws Exception {
    AssignPolicyRequest request = new AssignPolicyRequest(null, LocalDate.now());

    mockMvc.perform(
        post("/api/policies/{userId}/assign", USER_ID).header("Authorization", BEARER_TOKEN)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request))).andExpect(status().isBadRequest());
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void getHolidaysForPolicyListTest() throws Exception {
    when(configService.getHolidaysForPolicy(POLICY_ID)).thenReturn(List.of(sampleHolidayDto()));

    mockMvc.perform(
            get("/api/policies/{policyId}/holidays", POLICY_ID).header("Authorization",
                BEARER_TOKEN))
        .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1))
        .andExpect(jsonPath("$[0].holidayId").value(HOLIDAY_ID))
        .andExpect(jsonPath("$[0].name").value("Nowy Rok"));
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void assignHolidayToPolicyTest() throws Exception {
    AssignHolidayRequest request = new AssignHolidayRequest(HOLIDAY_ID);
    doNothing().when(configService)
        .assignHolidayToPolicy(eq(POLICY_ID), any(AssignHolidayRequest.class));

    mockMvc.perform(
        post("/api/policies/{policyId}/holidays", POLICY_ID).header("Authorization", BEARER_TOKEN)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request))).andExpect(status().isNoContent());
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void removeHolidayFromPolicyTest() throws Exception {
    doNothing().when(configService).removeHolidayFromPolicy(POLICY_ID, HOLIDAY_ID);

    mockMvc.perform(
        delete("/api/policies/{policyId}/holidays/{holidayId}", POLICY_ID, HOLIDAY_ID).header(
            "Authorization", BEARER_TOKEN)).andExpect(status().isNoContent());
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void assignPolicyBulkTest() throws Exception {
    MassAssignPolicyRequest request = new MassAssignPolicyRequest(
        POLICY_ID, List.of(1, 2, 3), LocalDate.now());
    doNothing().when(managementService).assignPolicyToUsersBulk(any(MassAssignPolicyRequest.class));

    mockMvc.perform(post("/api/policies/assign-bulk").header("Authorization", BEARER_TOKEN)
            .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isNoContent());
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void assignHolidaysToPolicyBulkTest() throws Exception {
    MassAssignHolidayRequest request = new MassAssignHolidayRequest(List.of(1, 2, 3));
    doNothing().when(configService)
        .assignHolidaysToPolicyBulk(eq(POLICY_ID), any(MassAssignHolidayRequest.class));

    mockMvc.perform(
            post("/api/policies/{policyId}/holidays/bulk", POLICY_ID).header(
                "Authorization",
                BEARER_TOKEN
            ).contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isNoContent());
  }
}
