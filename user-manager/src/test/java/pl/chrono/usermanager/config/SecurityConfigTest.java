package pl.chrono.usermanager.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import pl.chrono.usermanager.security.CustomUserDetailsService;
import pl.chrono.usermanager.security.JwtAuthenticationFilter;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = { SecurityConfig.class, SecurityConfigTest.MockConfig.class })
@WebAppConfiguration
class SecurityConfigTest {

  @Configuration
  @EnableWebMvc
  static class MockConfig {
    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter() {
      return mock(JwtAuthenticationFilter.class);
    }

    @Bean
    public CustomUserDetailsService customUserDetailsService() {
      return mock(CustomUserDetailsService.class);
    }
  }

  @Autowired private SecurityFilterChain securityFilterChain;
  @Autowired private PasswordEncoder passwordEncoder;
  @Autowired private AuthenticationProvider authenticationProvider;
  @Autowired private AuthenticationManager authenticationManager;
  @Autowired private CorsConfigurationSource corsConfigurationSource;

  @Test
  void contextLoadsAndSecurityBeansAreSuccessfullyCreated() {
    assertNotNull(securityFilterChain, "SecurityFilterChain powinien zostać wygenerowany");
    assertNotNull(passwordEncoder, "PasswordEncoder powinien zostać wygenerowany");
    assertNotNull(authenticationProvider, "AuthenticationProvider powinien zostać wygenerowany");
    assertNotNull(authenticationManager, "AuthenticationManager powinien zostać wygenerowany");
    assertNotNull(corsConfigurationSource, "CorsConfigurationSource powinien zostać wygenerowany");
  }
}
