package com.finance.tracker.controller.api;

import com.finance.tracker.dto.request.TransactionRequest;
import com.finance.tracker.dto.request.TransactionUpdateRequest;
import com.finance.tracker.dto.response.TransactionResponse;
import com.finance.tracker.exception.response.ErrorResponse;
import com.finance.tracker.exception.response.ValidationErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

@Tag(name = "Transaction Controller", description = "Transaction management endpoints")
public interface TransactionControllerApi {

    @Operation(summary = "Get transaction by ID", description = "Returns transaction details by identifier.")
    @ApiResponses(value = {
        @ApiResponse(
                responseCode = "200",
                description = "Transaction found",
                content = @Content(schema = @Schema(implementation = TransactionResponse.class))),
        @ApiResponse(
                responseCode = "404",
                description = "Transaction not found",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/api/v1/transactions/{id}")
    ResponseEntity<TransactionResponse> getById(
            @Parameter(description = "Transaction ID", required = true, example = "1")
            @PathVariable Long id
    );

    @Operation(
            summary = "Get transactions",
            description = "Returns all transactions or filters them by date range."
    )
    @ApiResponses(value = {
        @ApiResponse(
                responseCode = "200",
                description = "Transactions retrieved successfully",
                content = @Content(
                        array = @ArraySchema(schema = @Schema(implementation = TransactionResponse.class))
                )),
        @ApiResponse(
                responseCode = "400",
                description = "Invalid query parameters",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/api/v1/transactions")
    ResponseEntity<List<TransactionResponse>> getByDateRange(
            @Parameter(description = "Start date filter", example = "2026-03-01")
            @RequestParam(required = false) LocalDate startDate,
            @Parameter(description = "End date filter", example = "2026-03-31")
            @RequestParam(required = false) LocalDate endDate,
            @Parameter(description = "Whether to fetch transactions with EntityGraph", example = "false")
            @RequestParam(required = false, defaultValue = "false") boolean withEntityGraph
    );

    @Operation(summary = "Create transaction", description = "Creates a new transaction.")
    @ApiResponses(value = {
        @ApiResponse(
                responseCode = "201",
                description = "Transaction created successfully",
                content = @Content(schema = @Schema(implementation = TransactionResponse.class))),
        @ApiResponse(
                responseCode = "400",
                description = "Invalid request body",
                content = @Content(schema = @Schema(implementation = ValidationErrorResponse.class))),
        @ApiResponse(
                responseCode = "404",
                description = "Account or tags not found",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/api/v1/transactions")
    ResponseEntity<TransactionResponse> createTransaction(
            @Parameter(description = "Transaction payload", required = true)
            @Valid @RequestBody TransactionRequest request
    );

    @Operation(
            summary = "Bulk create transactions",
            description = "Imports a list of transactions in transactional or non-transactional mode."
    )
    @ApiResponses(value = {
        @ApiResponse(
                responseCode = "201",
                description = "Transactions created successfully",
                content = @Content(
                        array = @ArraySchema(schema = @Schema(implementation = TransactionResponse.class))
                )),
        @ApiResponse(
                responseCode = "400",
                description = "Invalid request body",
                content = @Content(schema = @Schema(implementation = ValidationErrorResponse.class))),
        @ApiResponse(
                responseCode = "404",
                description = "Account or tags not found",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/api/v1/transactions/bulk")
    ResponseEntity<List<TransactionResponse>> createTransactionsBulk(
            @Parameter(description = "List of transaction payloads", required = true)
            @Valid @RequestBody List<@Valid TransactionRequest> requests,
            @Parameter(description = "Run bulk import inside a transaction", example = "true")
            @RequestParam(defaultValue = "true") boolean transactional
    );

    @Operation(summary = "Patch transaction", description = "Partially updates a transaction.")
    @ApiResponses(value = {
        @ApiResponse(
                responseCode = "200",
                description = "Transaction updated successfully",
                content = @Content(schema = @Schema(implementation = TransactionResponse.class))),
        @ApiResponse(
                responseCode = "400",
                description = "Invalid request body",
                content = @Content(schema = @Schema(implementation = ValidationErrorResponse.class))),
        @ApiResponse(
                responseCode = "404",
                description = "Transaction, account, or tags not found",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PatchMapping("/api/v1/transactions/{id}")
    ResponseEntity<TransactionResponse> updateTransaction(
            @Parameter(description = "Transaction ID", required = true, example = "1")
            @PathVariable Long id,
            @Parameter(description = "Partial transaction payload", required = true)
            @Valid @RequestBody TransactionUpdateRequest request
    );

    @Operation(summary = "Delete transaction", description = "Deletes a transaction by ID.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Transaction deleted successfully"),
        @ApiResponse(
                responseCode = "404",
                description = "Transaction not found",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @DeleteMapping("/api/v1/transactions/{id}")
    ResponseEntity<Void> deleteTransaction(
            @Parameter(description = "Transaction ID", required = true, example = "1")
            @PathVariable Long id
    );
}
