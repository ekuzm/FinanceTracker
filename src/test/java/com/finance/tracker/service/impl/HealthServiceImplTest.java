package com.finance.tracker.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

@ExtendWith(MockitoExtension.class)
class HealthServiceImplTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    private HealthServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new HealthServiceImpl(jdbcTemplate, "finance-tracker");
    }

    @Test
    void getHealthShouldReturnUpWhenDatabaseQuerySucceeds() {
        when(jdbcTemplate.queryForObject("SELECT 1", Integer.class)).thenReturn(1);

        var response = service.getHealth();

        assertEquals("finance-tracker", response.getService());
        assertEquals("UP", response.getStatus());
        assertEquals("UP", response.getDatabase());
        assertNotNull(response.getTimestamp());
    }

    @Test
    void getHealthShouldReturnDownWhenDatabaseQueryFails() {
        when(jdbcTemplate.queryForObject("SELECT 1", Integer.class))
                .thenThrow(new RuntimeException("Database is unavailable"));

        var response = service.getHealth();

        assertEquals("finance-tracker", response.getService());
        assertEquals("DOWN", response.getStatus());
        assertEquals("DOWN", response.getDatabase());
        assertNotNull(response.getTimestamp());
    }
}
