package com.finance.tracker.service.impl;

import com.finance.tracker.dto.response.HealthResponse;
import com.finance.tracker.service.HealthService;
import java.time.LocalDateTime;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class HealthServiceImpl implements HealthService {

    private static final String STATUS_UP = "UP";
    private static final String STATUS_DOWN = "DOWN";

    private final JdbcTemplate jdbcTemplate;
    private final String applicationName;

    public HealthServiceImpl(
            JdbcTemplate jdbcTemplate,
            @Value("${spring.application.name}") String applicationName) {
        this.jdbcTemplate = jdbcTemplate;
        this.applicationName = applicationName;
    }

    @Override
    public HealthResponse getHealth() {
        boolean databaseUp = isDatabaseUp();
        String status = databaseUp ? STATUS_UP : STATUS_DOWN;

        return new HealthResponse(
                applicationName,
                status,
                status,
                LocalDateTime.now());
    }

    private boolean isDatabaseUp() {
        try {
            Integer result = jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            return Integer.valueOf(1).equals(result);
        } catch (Exception exception) {
            return false;
        }
    }
}
