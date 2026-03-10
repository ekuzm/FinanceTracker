package com.finance.tracker.controller.api;

import com.finance.tracker.domain.AccountType;
import com.finance.tracker.dto.request.UserRequest;
import com.finance.tracker.dto.request.UserUpdateRequest;
import com.finance.tracker.dto.response.UserResponse;
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
import java.math.BigDecimal;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

@Tag(name = "User Controller", description = "User management endpoints")
public interface UserControllerApi {

    @Operation(summary = "Get user by ID", description = "Returns user details by identifier.")
    @ApiResponses(value = {
        @ApiResponse(
                responseCode = "200",
                description = "User found",
                content = @Content(schema = @Schema(implementation = UserResponse.class))),
        @ApiResponse(
                responseCode = "404",
                description = "User not found",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/api/v1/users/{id}")
    ResponseEntity<UserResponse> getUserById(
            @Parameter(description = "User ID", required = true, example = "1")
            @PathVariable Long id
    );

    @Operation(summary = "Get all users", description = "Returns all users.")
    @ApiResponses(value = {
        @ApiResponse(
                responseCode = "200",
                description = "Users retrieved successfully",
                content = @Content(array = @ArraySchema(schema = @Schema(implementation = UserResponse.class))))
    })
    @GetMapping("/api/v1/users")
    ResponseEntity<List<UserResponse>> getAllUsers();

    @Operation(
            summary = "Search users with JPQL",
            description = "Finds users by account type and budget limit range using JPQL."
    )
    @ApiResponses(value = {
        @ApiResponse(
                responseCode = "200",
                description = "Users retrieved successfully",
                content = @Content(array = @ArraySchema(schema = @Schema(implementation = UserResponse.class)))),
        @ApiResponse(
                responseCode = "400",
                description = "Invalid search parameters",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/api/v1/users/search/account-type/jpql")
    ResponseEntity<List<UserResponse>> searchUsersWithJpql(
            @Parameter(description = "Account type filter", required = true, example = "CHECKING")
            @RequestParam("accountType") AccountType accountType,
            @Parameter(description = "Minimum budget limit", required = true, example = "100.00")
            @RequestParam("minBudgetLimit") BigDecimal minBudgetLimit,
            @Parameter(description = "Maximum budget limit", required = true, example = "1000.00")
            @RequestParam("maxBudgetLimit") BigDecimal maxBudgetLimit
    );

    @Operation(
            summary = "Search users with native SQL",
            description = "Finds users by account type and budget limit range using a native query."
    )
    @ApiResponses(value = {
        @ApiResponse(
                responseCode = "200",
                description = "Users retrieved successfully",
                content = @Content(array = @ArraySchema(schema = @Schema(implementation = UserResponse.class)))),
        @ApiResponse(
                responseCode = "400",
                description = "Invalid search parameters",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/api/v1/users/search/account-type/native")
    ResponseEntity<List<UserResponse>> searchUsersWithNative(
            @Parameter(description = "Account type filter", required = true, example = "CHECKING")
            @RequestParam("accountType") AccountType accountType,
            @Parameter(description = "Minimum budget limit", required = true, example = "100.00")
            @RequestParam("minBudgetLimit") BigDecimal minBudgetLimit,
            @Parameter(description = "Maximum budget limit", required = true, example = "1000.00")
            @RequestParam("maxBudgetLimit") BigDecimal maxBudgetLimit
    );

    @Operation(summary = "Create user", description = "Creates a new user.")
    @ApiResponses(value = {
        @ApiResponse(
                responseCode = "201",
                description = "User created successfully",
                content = @Content(schema = @Schema(implementation = UserResponse.class))),
        @ApiResponse(
                responseCode = "400",
                description = "Invalid request body",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(
                responseCode = "404",
                description = "Related accounts or budgets not found",
                content = @Content(schema = @Schema(implementation = ValidationErrorResponse.class))),
        @ApiResponse(
                responseCode = "409",
                description = "User relation conflict",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/api/v1/users")
    ResponseEntity<UserResponse> createUser(
            @Parameter(description = "User payload", required = true)
            @Valid @RequestBody UserRequest request
    );

    @Operation(summary = "Patch user", description = "Partially updates a user.")
    @ApiResponses(value = {
        @ApiResponse(
                responseCode = "200",
                description = "User updated successfully",
                content = @Content(schema = @Schema(implementation = UserResponse.class))),
        @ApiResponse(
                responseCode = "400",
                description = "Invalid request body",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(
                responseCode = "404",
                description = "User or related entities not found",
                content = @Content(schema = @Schema(implementation = ValidationErrorResponse.class))),
        @ApiResponse(
                responseCode = "409",
                description = "User relation conflict",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PatchMapping("/api/v1/users/{id}")
    ResponseEntity<UserResponse> updateUser(
            @Parameter(description = "User ID", required = true, example = "1")
            @PathVariable Long id,
            @Parameter(description = "Partial user payload", required = true)
            @Valid @RequestBody UserUpdateRequest request
    );

    @Operation(summary = "Delete user", description = "Deletes a user by ID.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "User deleted successfully"),
        @ApiResponse(
                responseCode = "404",
                description = "User not found",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @DeleteMapping("/api/v1/users/{id}")
    ResponseEntity<Void> deleteUser(
            @Parameter(description = "User ID", required = true, example = "1")
            @PathVariable Long id
    );
}
