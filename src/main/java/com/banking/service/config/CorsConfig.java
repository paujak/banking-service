package com.banking.service.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * CORS configuration active only in the {@code dev} profile.
 * Permits GET and POST requests from the local Angular development server ({@code http://localhost:4200}).
 */
@Configuration
@Profile("dev")
public class CorsConfig {

    /**
     * Registers a CORS mapping that allows the Angular dev server to call {@code /api/**} endpoints.
     *
     * @return a {@link WebMvcConfigurer} that applies the dev-only CORS policy
     */
    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/api/**")
                        .allowedOrigins("http://localhost:4200")
                        .allowedMethods("GET", "POST")
                        .allowedHeaders("*")
                        .allowCredentials(true);
            }
        };
    }
}
