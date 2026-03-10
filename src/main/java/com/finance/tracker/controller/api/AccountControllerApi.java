package com.finance.tracker.controller.api;

import com.finance.tracker.dto.request.AccountRequest;
import com.finance.tracker.dto.request.AccountTransferRequest;
import com.finance.tracker.dto.request.AccountUpdateRequest;
import com.finance.tracker.dto.response.AccountResponse;
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
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

@Tag(name = "Account Controller", description = "Account management endpoints")
public interface AccountControllerApi {

    @Operation(summary = "Get account by ID", description = "Returns account details by identifier.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Account found",
            content = @Content(schema = @Schema(implementation = AccountResponse.class))),
        @ApiResponse(responseCode = "404", description = "Account not found",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/api/v1/accounts/{id}")
    ResponseEntity<AccountResponse> getAccountById(
            @Parameter(description = "Account ID", required = true, example = "1") @PathVariable Long id);

    @Operation(summary = "Get all accounts", description = "Returns all accounts.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Accounts retrieved successfully", 
            content = @Content(array = @ArraySchema(schema = @Schema(implementation = AccountResponse.class))))
    })
    @GetMapping("/api/v1/accounts")
    ResponseEntity<List<AccountResponse>> getAllAccounts();

    @Operation(summary = "Create account", description = "Creates a new account.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Account created successfully",
            content = @Content(schema = @Schema(implementation = AccountResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid request body",
            content = @Content(schema = @Schema(implementation = ValidationErrorResponse.class))),
        @ApiResponse(responseCode = "404", description = "User not found",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/api/v1/accounts")
    ResponseEntity<AccountResponse> createAccount(
            @Parameter(description = "Account payload", required = true) @Valid @RequestBody AccountRequest request);

    @Operation(summary = "Create transfer", description = "Transfers money between two accounts.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Transfer completed successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request body",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "404", description = "Account not found", 
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "409", description = "Transfer conflict", 
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "500", description = "Unexpected transfer error", 
            content = @Content(schema = @Schema(implementation = ValidationErrorResponse.class)))
    })
    @PostMapping("/api/v1/account/transfer")
    ResponseEntity<Void> createTransfer(
        @Parameter(description = "Transfer payload", required = true) 
        @Valid @RequestBody AccountTransferRequest request,
        @Parameter(description = "Run transfer inside a transaction", example = "true")
        @RequestParam(defaultValue = "true") boolean transactional,
        @Parameter(description = "Force an error after debit for demo purposes", example = "false") 
        @RequestParam(defaultValue = "false") boolean failAfterDebit);

    @Operation(summary = "Patch account", description = "Partially updates an account.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Account updated successfully", 
            content = @Content(schema = @Schema(implementation = AccountResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid request body", 
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "404", description = "Account or user not found", 
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "409", description = "Account update conflict", 
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PatchMapping("/api/v1/accounts/{id}")
    ResponseEntity<AccountResponse> updateAccount(
        @Parameter(description = "Account ID", required = true, example = "1") 
        @PathVariable Long id,
        @Parameter(description = "Partial account payload", required = true) 
        @Valid @RequestBody AccountUpdateRequest request);

    @Operation(summary = "Delete account", description = "Deletes an account by ID.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Account deleted successfully"),
        @ApiResponse(responseCode = "404", description = "Account not found", 
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @DeleteMapping("/api/v1/accounts/{id}")
    ResponseEntity<Void> deleteAccount(
            @Parameter(description = "Account ID", required = true, example = "1") @PathVariable Long id);
}
