package pl.chrono.workforceapi.config;

import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration
@EntityScan("pl.chrono.dbmanager.model")
@EnableJpaRepositories("pl.chrono.dbmanager.repository")
public class JpaConfig {
}
