package com.finance.tracker.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import com.finance.tracker.dto.response.HealthResponse;
import com.finance.tracker.service.HealthService;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

@ExtendWith(MockitoExtension.class)
class HealthControllerTest {

    @Mock
    private HealthService healthService;

    private HealthController controller;

    @BeforeEach
    void setUp() {
        controller = new HealthController(healthService);
    }

    @Test
    void getHealthShouldReturnOkWhenApplicationIsUp() {
        when(healthService.getHealth()).thenReturn(new HealthResponse(
                "finance-tracker",
                "UP",
                "UP",
                LocalDateTime.now()));

        var response = controller.getHealth();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("UP", response.getBody().getStatus());
    }

    @Test
    void getHealthShouldReturnServiceUnavailableWhenApplicationIsDown() {
        when(healthService.getHealth()).thenReturn(new HealthResponse(
                "finance-tracker",
                "DOWN",
                "DOWN",
                LocalDateTime.now()));

        var response = controller.getHealth();

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.getStatusCode());
        assertEquals("DOWN", response.getBody().getStatus());
    }
}
