package com.banking.service;

import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
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
}
