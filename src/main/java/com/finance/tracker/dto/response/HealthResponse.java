package com.finance.tracker.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Application health response payload.")
public class HealthResponse {

    private String service;
    private String status;
    private String database;
    private LocalDateTime timestamp;
}
