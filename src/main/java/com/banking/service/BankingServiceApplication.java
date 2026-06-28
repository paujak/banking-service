package com.banking.service;

import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import tools.jackson.databind.ObjectMapper;

// TODO add logging
// TODO add javadoc comments on public methods and classes/records

import org.springframework.retry.annotation.EnableRetry;

@SpringBootApplication
@EnableRetry
public class BankingServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(BankingServiceApplication.class, args);
    }
    
    @Bean
    CloseableHttpClient httpClient() {
        return HttpClients.createDefault();
    }
    
    @Bean
    ObjectMapper objectMapper() {
        return new ObjectMapper();
    }
}
