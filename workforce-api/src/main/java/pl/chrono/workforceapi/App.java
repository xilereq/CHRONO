package pl.chrono.workforceapi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(scanBasePackages = "pl.chrono")
@EntityScan("pl.chrono.dbmanager.model")
@EnableJpaRepositories("pl.chrono.dbmanager.repository")
public class App {

    public static void main(String[] args) {
        SpringApplication.run(App.class, args);
    }

}
