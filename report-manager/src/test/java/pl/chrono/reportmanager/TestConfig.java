package pl.chrono.reportmanager;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EntityScan("pl.chrono.dbmanager.model")
@EnableJpaRepositories("pl.chrono.dbmanager.repository")
@ComponentScan(basePackages = {
    "pl.chrono.reportmanager",
    "pl.chrono.dbmanager",
    "pl.chrono.timemanager"
})
public class TestConfig {
}
