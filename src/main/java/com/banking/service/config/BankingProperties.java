package com.banking.service.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "banking")
@Validated
public record BankingProperties(
        @NotBlank String dbHost,
        @NotBlank String dbName,
        @NotBlank String dbUsername,
        @NotBlank String dbPassword,
        @NotBlank String sslKeystorePath,
        @NotBlank String sslKeystorePassword,
        @NotBlank String sslTruststorePath,
        @NotBlank String sslTruststorePassword
) {}
