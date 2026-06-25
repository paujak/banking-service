package com.banking.service.service;

import com.banking.service.dto.HealthResponse;
import org.springframework.boot.info.BuildProperties;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class HealthService {

    private final BuildProperties buildProperties;

    public HealthService(BuildProperties buildProperties) {
        this.buildProperties = buildProperties;
    }

    public HealthResponse health() {
        return new HealthResponse("UP", buildProperties.getVersion(), Instant.now());
    }
}
