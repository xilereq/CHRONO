package pl.chrono.workforceapi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "pl.chrono")
public class App {

    public static void main(String[] args) {
        SpringApplication.run(App.class, args);
    }

}
