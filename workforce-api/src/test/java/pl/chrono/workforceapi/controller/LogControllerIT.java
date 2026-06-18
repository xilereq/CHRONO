package pl.chrono.workforceapi.controller;

import java.util.List;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import pl.chrono.logmanager.dto.OperationLogDto;
import pl.chrono.logmanager.service.LogService;
import pl.chrono.usermanager.security.JwtService;
import pl.chrono.usermanager.service.AuthService;
import pl.chrono.workforceapi.TestApiConfig;

@WebMvcTest(LogController.class)
@Import(TestApiConfig.class)
@AutoConfigureMockMvc
class LogControllerIT {

  private static final String BEARER_TOKEN = "Bearer dummy.jwt.token";

  @Autowired
  private MockMvc mockMvc;

  @MockitoBean
  private LogService logService;

  @MockitoBean
  private AuthService authService;

  @MockitoBean
  private JwtService jwtService;

  @MockitoBean
  private UserDetailsService userDetailsService;

  @Test
  @WithMockUser(roles = "ADMIN")
  void getAllLogsAdminListTest() throws Exception {
    List<OperationLogDto> logs = List.of(
        mock(OperationLogDto.class),
        mock(OperationLogDto.class),
        mock(OperationLogDto.class)
    );
    when(logService.getAllOperationalLogs()).thenReturn(logs);

    mockMvc.perform(get("/api/logs/operational").header("Authorization", BEARER_TOKEN))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(3));

    verify(logService).getAllOperationalLogs();
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void getAllLogsEmptyListTest() throws Exception {
    when(logService.getAllOperationalLogs()).thenReturn(List.of());

    mockMvc.perform(get("/api/logs/operational").header("Authorization", BEARER_TOKEN))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(0));
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void getAllLogsSingleEntryTest() throws Exception {
    when(logService.getAllOperationalLogs()).thenReturn(List.of(mock(OperationLogDto.class)));

    mockMvc.perform(get("/api/logs/operational").header("Authorization", BEARER_TOKEN))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(1));
  }

  @Test
  @WithMockUser(roles = "MANAGER")
  void getAllLogsForbiddenForManagerTest() throws Exception {
    mockMvc.perform(get("/api/logs/operational").header("Authorization", BEARER_TOKEN))
        .andExpect(status().isForbidden());
  }

  @Test
  @WithMockUser(roles = "HR")
  void getAllLogsForbiddenForHrTest() throws Exception {
    mockMvc.perform(get("/api/logs/operational").header("Authorization", BEARER_TOKEN))
        .andExpect(status().isForbidden());
  }

  @Test
  @WithMockUser(roles = "EMPLOYEE")
  void getAllLogsForbiddenForEmployeeTest() throws Exception {
    mockMvc.perform(get("/api/logs/operational").header("Authorization", BEARER_TOKEN))
        .andExpect(status().isForbidden());
  }
}
