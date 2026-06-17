package pl.chrono.workforceapi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = "pl.chrono")
@EnableScheduling
@EnableAsync
public class App {
  public static void main(String[] args) {
    SpringApplication.run(App.class, args);
  }
}
