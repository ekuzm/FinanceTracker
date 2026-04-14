package com.finance.tracker.controller.api;

import com.finance.tracker.dto.response.HealthResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;

@Tag(name = "Health Controller", description = "Service health check endpoint")
public interface HealthControllerApi {

    @Operation(
            summary = "Get service health",
            description = "Returns the current application and database availability status."
    )
    @ApiResponses(value = {
        @ApiResponse(
                responseCode = "200",
                description = "Application is healthy",
                content = @Content(schema = @Schema(implementation = HealthResponse.class))),
        @ApiResponse(
                responseCode = "503",
                description = "Application is unavailable",
                content = @Content(schema = @Schema(implementation = HealthResponse.class)))
    })
    @GetMapping("/api/v1/health")
    ResponseEntity<HealthResponse> getHealth();
}
