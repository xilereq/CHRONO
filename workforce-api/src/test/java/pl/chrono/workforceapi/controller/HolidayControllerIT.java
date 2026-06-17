package pl.chrono.workforceapi.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import pl.chrono.timemanager.dto.request.CreateHolidayRequest;
import pl.chrono.timemanager.dto.response.HolidayDto;
import pl.chrono.timemanager.exception.HolidayException;
import pl.chrono.timemanager.service.HolidayService;
import pl.chrono.usermanager.security.JwtService;
import pl.chrono.usermanager.service.AuthService;
import pl.chrono.workforceapi.TestApiConfig;

@WebMvcTest(HolidayController.class)
@Import(TestApiConfig.class)
@AutoConfigureMockMvc(addFilters = false)
class HolidayControllerIT {

  private static final String BEARER_TOKEN = "Bearer dummy.jwt.token";
  private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
  @Autowired
  private MockMvc mockMvc;
  @MockitoBean
  private HolidayService holidayService;
  @MockitoBean
  private AuthService authService;
  @MockitoBean
  private JwtService jwtService;
  @MockitoBean
  private UserDetailsService userDetailsService;

  private HolidayDto sampleHolidayDto() {
    return new HolidayDto(1, "Boże Narodzenie", true, LocalDate.of(2025, 12, 25));
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void createHolidayValidTest() throws Exception {
    CreateHolidayRequest request = new CreateHolidayRequest(
        "Boże Narodzenie", true, LocalDate.of(2025, 12, 25));

    when(holidayService.createHoliday(any(CreateHolidayRequest.class))).thenReturn(
        sampleHolidayDto());

    mockMvc.perform(post("/api/holidays").header("Authorization", BEARER_TOKEN)
            .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk()).andExpect(jsonPath("$.holidayId").value(1))
        .andExpect(jsonPath("$.name").value("Boże Narodzenie"))
        .andExpect(jsonPath("$.occursYearly").value(true))
        .andExpect(jsonPath("$.date").value("2025-12-25"));

    verify(holidayService).createHoliday(any(CreateHolidayRequest.class));
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void createHolidayBlankNameTest() throws Exception {
    CreateHolidayRequest invalidRequest = new CreateHolidayRequest(
        "", false, LocalDate.of(2025, 5, 1));

    mockMvc.perform(post("/api/holidays").header("Authorization", BEARER_TOKEN)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(invalidRequest)))
        .andExpect(status().isBadRequest());
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void createHolidayNullDateTest() throws Exception {
    String jsonWithNullDate = "{\"name\":\"Nowy Rok\",\"occursYearly\":true,\"date\":null}";

    mockMvc.perform(post("/api/holidays").header("Authorization", BEARER_TOKEN)
            .contentType(MediaType.APPLICATION_JSON).content(jsonWithNullDate))
        .andExpect(status().isBadRequest());
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void getAllHolidaysHolidayListTest() throws Exception {
    HolidayDto second = new HolidayDto(2, "Nowy Rok", true, LocalDate.of(2025, 1, 1));

    when(holidayService.getAllHolidays()).thenReturn(List.of(sampleHolidayDto(), second));

    mockMvc.perform(get("/api/holidays").header("Authorization", BEARER_TOKEN))
        .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(2))
        .andExpect(jsonPath("$[0].holidayId").value(1))
        .andExpect(jsonPath("$[0].name").value("Boże Narodzenie"))
        .andExpect(jsonPath("$[1].holidayId").value(2))
        .andExpect(jsonPath("$[1].name").value("Nowy Rok"));
  }

  @Test
  @WithMockUser(roles = "MANAGER")
  void getAllHolidaysNoHolidayTest() throws Exception {
    when(holidayService.getAllHolidays()).thenReturn(List.of());

    mockMvc.perform(get("/api/holidays").header("Authorization", BEARER_TOKEN))
        .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void getHolidayByIdReturnTest() throws Exception {
    when(holidayService.getHolidayById(1)).thenReturn(sampleHolidayDto());

    mockMvc.perform(get("/api/holidays/{holidayId}", 1).header("Authorization", BEARER_TOKEN))
        .andExpect(status().isOk()).andExpect(jsonPath("$.holidayId").value(1))
        .andExpect(jsonPath("$.name").value("Boże Narodzenie"))
        .andExpect(jsonPath("$.occursYearly").value(true))
        .andExpect(jsonPath("$.date").value("2025-12-25"));
  }

  @Test
  @WithMockUser(roles = "MANAGER")
  void getHolidayByIdManagerTest() throws Exception {
    HolidayDto dto = new HolidayDto(7, "Wniebowzięcie NMP", true, LocalDate.of(2025, 8, 15));
    when(holidayService.getHolidayById(7)).thenReturn(dto);

    mockMvc.perform(get("/api/holidays/{holidayId}", 7).header("Authorization", BEARER_TOKEN))
        .andExpect(status().isOk()).andExpect(jsonPath("$.holidayId").value(7))
        .andExpect(jsonPath("$.name").value("Wniebowzięcie NMP"));
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void getHolidayByIdNotFoundTest() throws Exception {
    when(holidayService.getHolidayById(999)).thenThrow(new HolidayException.NotFound(999));

    mockMvc.perform(get("/api/holidays/{holidayId}", 999).header("Authorization", BEARER_TOKEN))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.message").value("Holiday with ID 999 does not exist!"));
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void deleteHolidayNoContentTest() throws Exception {
    doNothing().when(holidayService).deleteHoliday(1);

    mockMvc.perform(delete("/api/holidays/{holidayId}", 1).header("Authorization", BEARER_TOKEN))
        .andExpect(status().isNoContent());

    verify(holidayService).deleteHoliday(1);
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void deleteHolidayNotFoundTest() throws Exception {
    doThrow(new HolidayException.NotFound(404)).when(holidayService).deleteHoliday(404);

    mockMvc.perform(delete("/api/holidays/{holidayId}", 404).header("Authorization", BEARER_TOKEN))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.message").value("Holiday with ID 404 does not exist!"));
  }
}
