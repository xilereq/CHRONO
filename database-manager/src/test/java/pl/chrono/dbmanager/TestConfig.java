package pl.chrono.dbmanager;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EntityScan("pl.chrono.dbmanager.model")
@EnableJpaRepositories("pl.chrono.dbmanager.repository")
public class TestConfig {
}
