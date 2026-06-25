package com.banking.service.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.core.Ordered;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;

/**
 * Verifies database connectivity at startup using exponential backoff retry.
 * Terminates the application if the database cannot be reached after 10 attempts.
 */
@Component
public class DataSourceStartupListener implements ApplicationListener<ContextRefreshedEvent>, Ordered {

    private static final Logger log = LoggerFactory.getLogger(DataSourceStartupListener.class);

    private final DataSource dataSource;
    private volatile boolean verified = false;

    public DataSourceStartupListener(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        if (verified) {
            return;
        }
        verified = true;

        RetryTemplate retry = RetryTemplate.builder()
                .maxAttempts(10)
                .exponentialBackoff(1_000L, 2.0, 30_000L)
                .retryOn(Exception.class)
                .build();

        try {
            retry.execute(ctx -> {
                int attempt = ctx.getRetryCount() + 1;
                long delay = ctx.getRetryCount() == 0 ? 0L : (long) (1_000L * Math.pow(2.0, ctx.getRetryCount() - 1));
                log.info("Database connectivity check — attempt {}/10 (delay {}ms)", attempt, delay);
                try (Connection conn = dataSource.getConnection()) {
                    conn.isValid(5);
                }
                log.info("Database connectivity confirmed on attempt {}", attempt);
                return null;
            });
        } catch (Exception e) {
            throw new IllegalStateException(
                    "Database unreachable after 10 attempts — terminating.", e);
        }
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
