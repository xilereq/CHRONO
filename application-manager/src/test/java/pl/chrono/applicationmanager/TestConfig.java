package pl.chrono.applicationmanager;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import pl.chrono.usermanager.service.ManagerDelegationService;

@SpringBootApplication
@EntityScan("pl.chrono.dbmanager.model")
@EnableJpaRepositories("pl.chrono.dbmanager.repository")
@ComponentScan(basePackages = {
    "pl.chrono.applicationmanager",
    "pl.chrono.dbmanager",
    "pl.chrono.timemanager"
})
@Import(ManagerDelegationService.class)
public class TestConfig {
}
