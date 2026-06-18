package pl.chrono.workforceapi.controller;

import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import pl.chrono.reportmanager.api.I_ReportFacade;
import pl.chrono.reportmanager.dto.ReportFormat;
import pl.chrono.reportmanager.exception.ReportException;
import pl.chrono.usermanager.security.JwtService;
import pl.chrono.usermanager.service.AuthService;
import pl.chrono.workforceapi.TestApiConfig;

@WebMvcTest(ReportController.class)
@Import(TestApiConfig.class)
@AutoConfigureMockMvc(addFilters = false)
class ReportControllerIT {

  private static final String BEARER_TOKEN = "Bearer dummy.jwt.token";
  private static final Integer USER_ID = 1;

  @Autowired
  private MockMvc mockMvc;

  @MockitoBean
  private I_ReportFacade reportFacade;

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

  @Test
  @WithMockUser(roles = "ADMIN")
  void getMyTimesheetReportOkTest() throws Exception {
    LocalDate start = LocalDate.of(2026, 6, 1);
    LocalDate end = LocalDate.of(2026, 6, 30);
    byte[] mockCsvData = "Id,Hours\n1,160".getBytes();

    when(reportFacade.getEmployeeTimesheet(eq(USER_ID), eq(start), eq(end), eq(ReportFormat.CSV)))
        .thenReturn(mockCsvData);

    mockMvc.perform(get("/api/reports/timesheet/me")
            .header("Authorization", BEARER_TOKEN)
            .param("startDate", start.toString())
            .param("endDate", end.toString())
            .param("format", "CSV"))
        .andExpect(status().isOk())
        .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"my_timesheet_2026-06-01_to_2026-06-30.csv\""))
        .andExpect(content().contentType("text/csv;charset=UTF-8"))
        .andExpect(content().bytes(mockCsvData));
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void getMyTimesheetReportMissingParamTest() throws Exception {
    mockMvc.perform(get("/api/reports/timesheet/me")
            .header("Authorization", BEARER_TOKEN)
            .param("startDate", "2026-06-01"))
        .andExpect(status().isBadRequest());
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void getEmployeeTimesheetReportOkTest() throws Exception {
    Integer employeeId = 5;
    LocalDate start = LocalDate.of(2026, 5, 1);
    LocalDate end = LocalDate.of(2026, 5, 31);
    byte[] mockPdfData = new byte[]{1, 2, 3};

    when(reportFacade.getEmployeeTimesheet(eq(employeeId), eq(start), eq(end), eq(ReportFormat.PDF)))
        .thenReturn(mockPdfData);

    mockMvc.perform(get("/api/reports/timesheet/employee/{employeeId}", employeeId)
            .header("Authorization", BEARER_TOKEN)
            .param("startDate", start.toString())
            .param("endDate", end.toString())
            .param("format", "PDF"))
        .andExpect(status().isOk())
        .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"employee_5_timesheet_2026-05-01_to_2026-05-31.pdf\""))
        .andExpect(content().contentType(MediaType.APPLICATION_PDF));
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void getEmployeeTimesheetReportExceptionTest() throws Exception {
    Integer employeeId = 99;
    when(reportFacade.getEmployeeTimesheet(eq(employeeId), any(), any(), any()))
        .thenThrow(new ReportException("Employee not found", "REP_EMP_NOT_FOUND", HttpStatus.NOT_FOUND));

    mockMvc.perform(get("/api/reports/timesheet/employee/{employeeId}", employeeId)
            .header("Authorization", BEARER_TOKEN)
            .param("startDate", "2026-05-01")
            .param("endDate", "2026-05-31"))
        .andExpect(status().isNotFound());
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void getMyOvertimeReportOkTest() throws Exception {
    mockMvc.perform(get("/api/reports/overtime/me")
            .header("Authorization", BEARER_TOKEN)
            .param("startDate", "2026-01-01")
            .param("endDate", "2026-01-31"))
        .andExpect(status().isOk());
  }

  @Test
  @WithMockUser(roles = "MANAGER")
  void getTeamWorkloadReportOkTest() throws Exception {
    mockMvc.perform(get("/api/reports/workload/team")
            .header("Authorization", BEARER_TOKEN)
            .param("startDate", "2026-02-01")
            .param("endDate", "2026-02-28"))
        .andExpect(status().isOk());
  }

  @Test
  @WithMockUser(roles = "MANAGER")
  void getTeamAbsencesReportOkTest() throws Exception {
    mockMvc.perform(get("/api/reports/absences/team")
            .header("Authorization", BEARER_TOKEN)
            .param("startDate", "2026-03-01")
            .param("endDate", "2026-03-31"))
        .andExpect(status().isOk());
  }

  @Test
  @WithMockUser(roles = "MANAGER")
  void getTeamOvertimeReportOkTest() throws Exception {
    byte[] mockXlsxData = new byte[]{9, 8, 7};
    when(reportFacade.getTeamOvertime(any(), any(), any(), eq(ReportFormat.XLSX)))
        .thenReturn(mockXlsxData);

    mockMvc.perform(get("/api/reports/overtime/team")
            .header("Authorization", BEARER_TOKEN)
            .param("startDate", "2026-03-01")
            .param("endDate", "2026-03-31")
            .param("format", "XLSX"))
        .andExpect(status().isOk())
        .andExpect(content().contentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
  }

  @Test
  @WithMockUser(roles = "MANAGER")
  void getTeamAnomalyReportOkTest() throws Exception {
    mockMvc.perform(get("/api/reports/anomalies/team")
            .header("Authorization", BEARER_TOKEN)
            .param("startDate", "2026-04-01")
            .param("endDate", "2026-04-30"))
        .andExpect(status().isOk());
  }
}
