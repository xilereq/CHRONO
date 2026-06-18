package pl.chrono.workforceapi.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import pl.chrono.usermanager.dto.request.AuthRequest;
import pl.chrono.usermanager.dto.request.ChangePasswordRequest;
import pl.chrono.usermanager.dto.request.PasswordResetConfirmRequest;
import pl.chrono.usermanager.dto.request.PasswordResetRequest;
import pl.chrono.usermanager.dto.request.RegisterRequest;
import pl.chrono.usermanager.dto.request.TokenRequest;
import pl.chrono.usermanager.dto.response.AuthResponse;
import pl.chrono.usermanager.dto.response.RegisterResponse;
import pl.chrono.usermanager.security.JwtService;
import pl.chrono.usermanager.service.AuthService;
import pl.chrono.workforceapi.TestApiConfig;

@WebMvcTest(AuthController.class)
@Import(TestApiConfig.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerIT {

  private static final String BEARER_TOKEN = "Bearer dummy.jwt.token";
  private final ObjectMapper objectMapper = new ObjectMapper();

  @Autowired
  private MockMvc mockMvc;

  @MockitoBean
  private AuthService authService;

  @MockitoBean
  private JwtService jwtService;

  @MockitoBean
  private UserDetailsService userDetailsService;

  @Test
  void loginOkTest() throws Exception {
    when(authService.login(any(AuthRequest.class))).thenReturn(mock(AuthResponse.class));

    mockMvc.perform(post("/api/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"email": "user@example.com", "password": "secret123"}
                """))
        .andExpect(status().isOk());

    verify(authService).login(any(AuthRequest.class));
  }

  @Test
  void loginInvalidCredentials400Test() throws Exception {
    when(authService.login(any(AuthRequest.class)))
        .thenThrow(new IllegalArgumentException("Invalid credentials"));

    mockMvc.perform(post("/api/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"email": "user@example.com", "password": "wrong"}
                """))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("Invalid credentials"));
  }

  @Test
  void extractUserIdFromTokenOkTest() throws Exception {
    when(authService.extractUserIdFromToken("some.valid.token")).thenReturn(42);

    mockMvc.perform(post("/api/auth/extract-user-id")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(new TokenRequest("some.valid.token"))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").value(42));
  }

  @Test
  void extractUserIdFromTokenReturnsCorrectIdTest() throws Exception {
    when(authService.extractUserIdFromToken("another.token")).thenReturn(99);

    mockMvc.perform(post("/api/auth/extract-user-id")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(new TokenRequest("another.token"))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").value(99));
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void registerOkTest() throws Exception {
    when(authService.registerNewUser(any(RegisterRequest.class)))
        .thenReturn(mock(RegisterResponse.class));

    mockMvc.perform(post("/api/auth/register")
            .header("Authorization", BEARER_TOKEN)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"email": "new@example.com", "firstName": "Jan",
                 "lastName": "Kowalski", "role": "EMPLOYEE"}
                """))
        .andExpect(status().isOk());

    verify(authService).registerNewUser(any(RegisterRequest.class));
  }

  @Test
  void changeFirstPasswordOkTest() throws Exception {
    doNothing().when(authService)
        .changeFirstPassword(anyString(), any(ChangePasswordRequest.class));

    // Używamy .principal() zamiast .with(user()) przy wyłączonych filtrach
    mockMvc.perform(post("/api/auth/change-first-password")
            .principal(new UsernamePasswordAuthenticationToken("user@example.com", null))
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"oldPassword": "OldPass1!", "newPassword": "NewPass2@"}
                """))
        .andExpect(status().isOk());

    verify(authService).changeFirstPassword(
        eq("user@example.com"), any(ChangePasswordRequest.class));
  }

  @Test
  void changeFirstPasswordWrongPassword400Test() throws Exception {
    doThrow(new IllegalArgumentException("Wrong current password"))
        .when(authService).changeFirstPassword(anyString(), any(ChangePasswordRequest.class));

    mockMvc.perform(post("/api/auth/change-first-password")
            .principal(new UsernamePasswordAuthenticationToken("user@example.com", null))
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"oldPassword": "wrong", "newPassword": "NewPass2@"}
                """))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("Wrong current password"));
  }

  @Test
  void forgotPasswordOkTest() throws Exception {
    doNothing().when(authService).requestPasswordReset(anyString());

    mockMvc.perform(post("/api/auth/forgot-password")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(new PasswordResetRequest("user@example.com"))))
        .andExpect(status().isOk());

    verify(authService).requestPasswordReset("user@example.com");
  }

  @Test
  void forgotPasswordNonExistentEmailOkTest() throws Exception {
    doNothing().when(authService).requestPasswordReset(anyString());

    mockMvc.perform(post("/api/auth/forgot-password")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(
                new PasswordResetRequest("nonexistent@example.com"))))
        .andExpect(status().isOk());
  }

  @Test
  void resetPasswordOkTest() throws Exception {
    doNothing().when(authService).resetPassword(anyString(), anyString());

    mockMvc.perform(post("/api/auth/reset-password")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(
                new PasswordResetConfirmRequest("reset.token.abc", "NewPass3#"))))
        .andExpect(status().isOk());

    verify(authService).resetPassword("reset.token.abc", "NewPass3#");
  }

  @Test
  void resetPasswordExpiredToken400Test() throws Exception {
    doThrow(new IllegalArgumentException("Token expired or invalid"))
        .when(authService).resetPassword(anyString(), anyString());

    mockMvc.perform(post("/api/auth/reset-password")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(
                new PasswordResetConfirmRequest("expired.token", "NewPass3#"))))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("Token expired or invalid"));
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void changePasswordOkTest() throws Exception {
    doNothing().when(authService).changePassword(anyString(), any(ChangePasswordRequest.class));

    mockMvc.perform(post("/api/auth/change-password")
            .principal(new UsernamePasswordAuthenticationToken("user@example.com", null))
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"oldPassword": "OldPass1!", "newPassword": "NewPass2@"}
                """))
        .andExpect(status().isOk());

    verify(authService).changePassword(
        eq("user@example.com"), any(ChangePasswordRequest.class));
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void changePasswordWrongCurrentPassword400Test() throws Exception {
    doThrow(new IllegalArgumentException("Incorrect current password"))
        .when(authService).changePassword(anyString(), any(ChangePasswordRequest.class));

    mockMvc.perform(post("/api/auth/change-password")
            .principal(new UsernamePasswordAuthenticationToken("user@example.com", null))
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"oldPassword": "wrongCurrent", "newPassword": "NewPass2@"}
                """))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("Incorrect current password"));
  }
}
