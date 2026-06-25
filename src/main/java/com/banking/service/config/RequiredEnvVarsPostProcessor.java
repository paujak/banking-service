package com.banking.service.config;

import org.springframework.boot.EnvironmentPostProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * Fail-fast validation of required environment variables before any subsystem initialises.
 * Runs after ConfigDataEnvironmentPostProcessor so that test profile YAML overrides are visible.
 */
public class RequiredEnvVarsPostProcessor implements EnvironmentPostProcessor, Ordered {

    private static final List<String> REQUIRED_VARS = List.of(
            "DB_HOST",
            "DB_NAME",
            "DB_USERNAME",
            "DB_PASSWORD",
            "SSL_KEYSTORE_PATH",
            "SSL_KEYSTORE_PASSWORD",
            "SSL_TRUSTSTORE_PATH",
            "SSL_TRUSTSTORE_PASSWORD"
    );

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment env, SpringApplication app) {
        List<String> missing = REQUIRED_VARS.stream()
                .filter(v -> !StringUtils.hasText(env.getProperty(v)))
                .toList();

        if (!missing.isEmpty()) {
            throw new IllegalStateException(
                    "Required environment variables not set: " + missing
            );
        }
    }

    @Override
    public int getOrder() {
        // Run after ConfigDataEnvironmentPostProcessor (HIGHEST_PRECEDENCE + 10)
        // so that profile-specific YAML values are available during the check.
        return Ordered.HIGHEST_PRECEDENCE + 100;
    }
}
