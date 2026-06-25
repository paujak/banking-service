package com.banking.service.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
class DatabaseConnectivityIT {

    @Container
    @ServiceConnection
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.4");

    @Autowired
    JdbcClient jdbcClient;

    @Test
    void selectOneExecutesWithoutException() {
        assertThatCode(() -> jdbcClient.sql("SELECT 1").query(Integer.class).single())
                .doesNotThrowAnyException();
    }

    @Test
    void flywayBaselineMigrationApplied() {
        record FlywayRow(String version, int success) {}
        List<FlywayRow> rows = jdbcClient
                .sql("SELECT version, success FROM flyway_schema_history ORDER BY installed_rank")
                .query((rs, rowNum) -> new FlywayRow(rs.getString("version"), rs.getInt("success")))
                .list();

        assertThat(rows).hasSize(1);
        assertThat(rows.get(0).version()).isEqualTo("1");
        assertThat(rows.get(0).success()).isEqualTo(1);
    }
}
