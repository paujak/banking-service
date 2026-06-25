package com.banking.service.config;

import org.flywaydb.core.Flyway;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

/**
 * Explicit Flyway configuration required because Spring Boot 4.x no longer includes
 * FlywayAutoConfiguration in spring-boot-autoconfigure. Each technology stack manages
 * its own autoconfiguration in dedicated modules, and Flyway integration must be
 * configured manually.
 */
@Configuration(proxyBeanMethods = false)
public class FlywayConfiguration {

    @Bean(initMethod = "migrate")
    @ConditionalOnProperty(name = "spring.flyway.enabled", havingValue = "true", matchIfMissing = true)
    public Flyway flyway(DataSource dataSource) {
        return Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration")
                .validateOnMigrate(true)
                .baselineOnMigrate(false)
                .outOfOrder(false)
                .load();
    }
}
