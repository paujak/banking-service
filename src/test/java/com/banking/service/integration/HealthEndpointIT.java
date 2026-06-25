package com.banking.service.integration;

import com.banking.service.dto.HealthResponse;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.client.RestClient;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@ActiveProfiles("test")
class HealthEndpointIT {

    @Container
    @ServiceConnection
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.4");

    @LocalServerPort
    int port;

    @Test
    void healthEndpointReturnsUp() {
        RestClient restClient = RestClient.create("http://localhost:" + port);

        ResponseEntity<HealthResponse> response = restClient.get()
                .uri("/health")
                .retrieve()
                .toEntity(HealthResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo("UP");
        assertThat(response.getBody().version()).isNotBlank();
        assertThat(response.getBody().timestamp()).isNotNull();
    }
}
