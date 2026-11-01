package pl.chrono.workforceapi;

import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import pl.chrono.usermanager.api.I_ManagerDelegationService;

@TestConfiguration
@EnableMethodSecurity
public class TestApiConfig {

  @Bean
  public I_ManagerDelegationService managerDelegationService() {
    return Mockito.mock(I_ManagerDelegationService.class);
  }
}
