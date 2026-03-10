package com.finance.tracker.controller.api;

import com.finance.tracker.dto.request.BudgetRequest;
import com.finance.tracker.dto.request.BudgetUpdateRequest;
import com.finance.tracker.dto.response.BudgetResponse;
import com.finance.tracker.exception.response.ErrorResponse;
import com.finance.tracker.exception.response.ValidationErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

@Tag(name = "Budget Controller", description = "Budget management endpoints")
public interface BudgetControllerApi {

    @Operation(summary = "Get budget by ID", description = "Returns budget details by identifier.")
    @ApiResponses(value = {
        @ApiResponse(
                responseCode = "200",
                description = "Budget found",
                content = @Content(schema = @Schema(implementation = BudgetResponse.class))),
        @ApiResponse(
                responseCode = "404",
                description = "Budget not found",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/api/v1/budgets/{id}")
    ResponseEntity<BudgetResponse> getBudgetById(
            @Parameter(description = "Budget ID", required = true, example = "1")
            @PathVariable Long id
    );

    @Operation(summary = "Get budgets", description = "Returns budgets with pagination and sorting.")
    @ApiResponses(value = {
        @ApiResponse(
                responseCode = "200",
                description = "Budgets retrieved successfully",
                content = @Content(schema = @Schema(implementation = Page.class)))
    })
    @GetMapping("/api/v1/budgets")
    ResponseEntity<Page<BudgetResponse>> getAllBudgets(
            @Parameter(description = "Page index", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size", example = "3")
            @RequestParam(defaultValue = "3") int size,
            @Parameter(description = "Sort field", example = "id")
            @RequestParam(defaultValue = "id") String sortBy,
            @Parameter(description = "Sort direction flag", example = "true")
            @RequestParam(defaultValue = "true") boolean ascending
    );

    @Operation(summary = "Create budget", description = "Creates a new budget.")
    @ApiResponses(value = {
        @ApiResponse(
                responseCode = "201",
                description = "Budget created successfully",
                content = @Content(schema = @Schema(implementation = BudgetResponse.class))),
        @ApiResponse(
                responseCode = "400",
                description = "Invalid request body",
                content = @Content(schema = @Schema(implementation = ValidationErrorResponse.class))),
        @ApiResponse(
                responseCode = "404",
                description = "User not found",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/api/v1/budgets")
    ResponseEntity<BudgetResponse> createBudget(
            @Parameter(description = "Budget payload", required = true)
            @Valid @RequestBody BudgetRequest request
    );

    @Operation(summary = "Patch budget", description = "Partially updates a budget.")
    @ApiResponses(value = {
        @ApiResponse(
                responseCode = "200",
                description = "Budget updated successfully",
                content = @Content(schema = @Schema(implementation = BudgetResponse.class))),
        @ApiResponse(
                responseCode = "400",
                description = "Invalid request body",
                content = @Content(schema = @Schema(implementation = ValidationErrorResponse.class))),
        @ApiResponse(
                responseCode = "404",
                description = "Budget or user not found",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PatchMapping("/api/v1/budgets/{id}")
    ResponseEntity<BudgetResponse> updateBudget(
            @Parameter(description = "Budget ID", required = true, example = "1")
            @PathVariable Long id,
            @Parameter(description = "Partial budget payload", required = true)
            @Valid @RequestBody BudgetUpdateRequest request
    );

    @Operation(summary = "Delete budget", description = "Deletes a budget by ID.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Budget deleted successfully"),
        @ApiResponse(
                responseCode = "404",
                description = "Budget not found",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @DeleteMapping("/api/v1/budgets/{id}")
    ResponseEntity<Void> deleteBudget(
            @Parameter(description = "Budget ID", required = true, example = "1")
            @PathVariable Long id
    );
}
