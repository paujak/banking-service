package com.banking.service;

import com.banking.service.config.BankingProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(BankingProperties.class)
public class BankingServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(BankingServiceApplication.class, args);
    }
}
