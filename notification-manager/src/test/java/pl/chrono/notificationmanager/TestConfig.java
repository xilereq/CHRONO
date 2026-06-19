package pl.chrono.notificationmanager;

import org.mockito.Mockito;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.mail.javamail.JavaMailSender;

@SpringBootApplication
@EntityScan("pl.chrono.dbmanager.model")
@EnableJpaRepositories("pl.chrono.dbmanager.repository")
@ComponentScan(basePackages = {"pl.chrono.notificationmanager", "pl.chrono.dbmanager"})
public class TestConfig {

  @Bean
  public JavaMailSender javaMailSender() {
    return Mockito.mock(JavaMailSender.class);
  }
}
