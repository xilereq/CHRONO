package pl.chrono.workforceapi.controller;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
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
import pl.chrono.usermanager.api.I_UserService;
import pl.chrono.usermanager.dto.response.UserDto;
import pl.chrono.usermanager.security.JwtAuthenticationFilter;
import pl.chrono.usermanager.security.JwtService;
import pl.chrono.usermanager.service.AuthService;
import pl.chrono.workforceapi.TestApiConfig;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;

@WebMvcTest(UserController.class)
@Import(TestApiConfig.class)
@AutoConfigureMockMvc
class UserControllerIT {

  private static final String BEARER_TOKEN = "Bearer dummy.jwt.token";
  private static final Integer USER_ID = 5;

  @Autowired
  private MockMvc mockMvc;

  @MockitoBean
  private I_UserService userService;

  @MockitoBean
  private AuthService authService;

  @MockitoBean
  private JwtService jwtService;

  @MockitoBean
  private UserDetailsService userDetailsService;

  @MockitoBean
  private JwtAuthenticationFilter jwtAuthenticationFilter;

  @BeforeEach
  void setUp() throws Exception {
    doAnswer(inv -> {
      FilterChain chain = inv.getArgument(2);
      chain.doFilter(inv.getArgument(0), inv.getArgument(1));
      return null;
    }).when(jwtAuthenticationFilter)
        .doFilter(any(ServletRequest.class), any(ServletResponse.class), any(FilterChain.class));

    when(authService.extractUserIdFromToken(anyString())).thenReturn(USER_ID);
  }

  @Test
  @WithMockUser(roles = "MANAGER")
  void getSubordinatesListTest() throws Exception {
    List<UserDto> subordinates = List.of(mock(UserDto.class), mock(UserDto.class));
    when(userService.getSubordinatesForManager(USER_ID)).thenReturn(subordinates);

    mockMvc.perform(get("/api/users/subordinates").header("Authorization", BEARER_TOKEN))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(2));
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void getSubordinatesAdminListTest() throws Exception {
    List<UserDto> subordinates = List.of(mock(UserDto.class));
    when(userService.getSubordinatesForManager(USER_ID)).thenReturn(subordinates);

    mockMvc.perform(get("/api/users/subordinates").header("Authorization", BEARER_TOKEN))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(1));
  }

  @Test
  @WithMockUser(roles = "MANAGER")
  void getSubordinatesEmptyListTest() throws Exception {
    when(userService.getSubordinatesForManager(USER_ID)).thenReturn(List.of());

    mockMvc.perform(get("/api/users/subordinates").header("Authorization", BEARER_TOKEN))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(0));
  }

  @Test
  @WithMockUser(roles = "MANAGER")
  void getSubordinatesMissingTokenTest() throws Exception {
    mockMvc.perform(get("/api/users/subordinates"))
        .andExpect(status().isBadRequest());
  }

  @Test
  @WithMockUser(roles = "EMPLOYEE")
  void getSubordinatesForbiddenForEmployeeTest() throws Exception {
    mockMvc.perform(get("/api/users/subordinates").header("Authorization", BEARER_TOKEN))
        .andExpect(status().isForbidden());
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void getManagersListAdminTest() throws Exception {
    when(userService.getAllManagers()).thenReturn(List.of(mock(UserDto.class), mock(UserDto.class)));

    mockMvc.perform(get("/api/users/managers"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(2));
  }

  @Test
  @WithMockUser(roles = "HR")
  void getManagersListHrTest() throws Exception {
    when(userService.getAllManagers()).thenReturn(List.of(mock(UserDto.class)));

    mockMvc.perform(get("/api/users/managers"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(1));
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void getManagersEmptyListTest() throws Exception {
    when(userService.getAllManagers()).thenReturn(List.of());

    mockMvc.perform(get("/api/users/managers"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(0));
  }

  @Test
  @WithMockUser(roles = "EMPLOYEE")
  void getManagersForbiddenForEmployeeTest() throws Exception {
    mockMvc.perform(get("/api/users/managers"))
        .andExpect(status().isForbidden());
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void getEmployeesListAdminTest() throws Exception {
    when(userService.getAllEmployees())
        .thenReturn(List.of(mock(UserDto.class), mock(UserDto.class), mock(UserDto.class)));

    mockMvc.perform(get("/api/users/employees"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(3));
  }

  @Test
  @WithMockUser(roles = "HR")
  void getEmployeesListHrTest() throws Exception {
    when(userService.getAllEmployees()).thenReturn(List.of(mock(UserDto.class)));

    mockMvc.perform(get("/api/users/employees"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(1));
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void getEmployeesEmptyListTest() throws Exception {
    when(userService.getAllEmployees()).thenReturn(List.of());

    mockMvc.perform(get("/api/users/employees"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(0));
  }

  @Test
  @WithMockUser(roles = "MANAGER")
  void getEmployeesForbiddenForManagerTest() throws Exception {
    mockMvc.perform(get("/api/users/employees"))
        .andExpect(status().isForbidden());
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void getAdminsListTest() throws Exception {
    when(userService.getAllAdmins()).thenReturn(List.of(mock(UserDto.class)));

    mockMvc.perform(get("/api/users/admins"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(1));
  }

  @Test
  @WithMockUser(roles = "HR")
  void getAdminsListHrTest() throws Exception {
    when(userService.getAllAdmins()).thenReturn(List.of(mock(UserDto.class)));

    mockMvc.perform(get("/api/users/admins"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(1));
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void getAdminsEmptyListTest() throws Exception {
    when(userService.getAllAdmins()).thenReturn(List.of());

    mockMvc.perform(get("/api/users/admins"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(0));
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void getHrsListAdminTest() throws Exception {
    when(userService.getAllHrs()).thenReturn(List.of(mock(UserDto.class), mock(UserDto.class)));

    mockMvc.perform(get("/api/users/hrs"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(2));
  }

  @Test
  @WithMockUser(roles = "HR")
  void getHrsListHrTest() throws Exception {
    when(userService.getAllHrs()).thenReturn(List.of(mock(UserDto.class)));

    mockMvc.perform(get("/api/users/hrs"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(1));
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void getHrsEmptyListTest() throws Exception {
    when(userService.getAllHrs()).thenReturn(List.of());

    mockMvc.perform(get("/api/users/hrs"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(0));
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void getAllUsersListAdminTest() throws Exception {
    when(userService.getAllUsers()).thenReturn(
        List.of(mock(UserDto.class), mock(UserDto.class), mock(UserDto.class)));

    mockMvc.perform(get("/api/users/users"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(3));
  }

  @Test
  @WithMockUser(roles = "HR")
  void getAllUsersListHrTest() throws Exception {
    when(userService.getAllUsers()).thenReturn(List.of(mock(UserDto.class)));

    mockMvc.perform(get("/api/users/users"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(1));
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void getAllUsersEmptyListTest() throws Exception {
    when(userService.getAllUsers()).thenReturn(List.of());

    mockMvc.perform(get("/api/users/users"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(0));
  }

  @Test
  @WithMockUser(roles = "MANAGER")
  void getAllUsersForbiddenForManagerTest() throws Exception {
    mockMvc.perform(get("/api/users/users"))
        .andExpect(status().isForbidden());
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void setManagerOkAdminTest() throws Exception {
    doNothing().when(userService).setManager(anyString(), anyString());

    mockMvc.perform(post("/api/users/set-manager")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"email": "employee@example.com", "managerEmail": "manager@example.com"}
                """))
        .andExpect(status().isOk());

    verify(userService).setManager("employee@example.com", "manager@example.com");
  }

  @Test
  @WithMockUser(roles = "HR")
  void setManagerOkHrTest() throws Exception {
    doNothing().when(userService).setManager(anyString(), anyString());

    mockMvc.perform(post("/api/users/set-manager")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"email": "employee@example.com", "managerEmail": "manager@example.com"}
                """))
        .andExpect(status().isOk());
  }

  @Test
  @WithMockUser(roles = "MANAGER")
  void setManagerForbiddenForManagerTest() throws Exception {
    mockMvc.perform(post("/api/users/set-manager")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"email": "employee@example.com", "managerEmail": "manager@example.com"}
                """))
        .andExpect(status().isForbidden());
  }
}
