package com.banking.service;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import tools.jackson.databind.ObjectMapper;

// TODO add logging

import org.springframework.retry.annotation.EnableRetry;

/**
 * Entry point for the Banking Service application.
 * Bootstraps Spring Boot, enables retry support, and registers shared infrastructure beans.
 */
@SpringBootApplication
@EnableRetry
public class BankingServiceApplication {

    /**
     * Starts the Spring Boot application.
     *
     * @param args command-line arguments passed to the application context
     */
    public static void main(String[] args) {
        SpringApplication.run(BankingServiceApplication.class, args);
    }

    /**
     * Creates the shared Apache HC5 HTTP client used for outbound HTTP calls.
     *
     * @return a default {@link CloseableHttpClient} instance
     */
    @Bean
    CloseableHttpClient httpClient() {
        return HttpClients.createDefault();
    }

    /**
     * Creates the shared Jackson {@link ObjectMapper} used for JSON serialisation.
     *
     * @return a default ObjectMapper instance
     */
    @Bean
    ObjectMapper objectMapper() {
        return new ObjectMapper();
    }

    /**
     * Configures the OpenAPI metadata and default server URL for Swagger UI.
     *
     * @return OpenAPI definition for the Banking Service
     */
    @Bean
    OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Banking Service API")
                        .description("REST API for managing bank accounts and financial transactions")
                        .version("1.0.0"))
                .addServersItem(new Server()
                        .url("https://localhost:8443")
                        .description("Local development (HTTPS)"));
    }
}
