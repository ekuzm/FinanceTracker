package com.finance.tracker.controller.api;

import com.finance.tracker.dto.request.TagRequest;
import com.finance.tracker.dto.request.TagUpdateRequest;
import com.finance.tracker.dto.response.TagResponse;
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

@Tag(name = "Tag Controller", description = "Tag management endpoints")
public interface TagControllerApi {

    @Operation(summary = "Get tag by ID", description = "Returns tag details by identifier.")
    @ApiResponses(value = {
        @ApiResponse(
                responseCode = "200",
                description = "Tag found",
                content = @Content(schema = @Schema(implementation = TagResponse.class))),
        @ApiResponse(
                responseCode = "404",
                description = "Tag not found",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/api/v1/tags/{id}")
    ResponseEntity<TagResponse> getTagById(
            @Parameter(description = "Tag ID", required = true, example = "1")
            @PathVariable Long id
    );

    @Operation(summary = "Get all tags", description = "Returns all tags.")
    @ApiResponses(value = {
        @ApiResponse(
                responseCode = "200",
                description = "Tags retrieved successfully",
                content = @Content(array = @ArraySchema(schema = @Schema(implementation = TagResponse.class))))
    })
    @GetMapping("/api/v1/tags")
    ResponseEntity<List<TagResponse>> getAllTags();

    @Operation(summary = "Create tag", description = "Creates a new tag.")
    @ApiResponses(value = {
        @ApiResponse(
                responseCode = "201",
                description = "Tag created successfully",
                content = @Content(schema = @Schema(implementation = TagResponse.class))),
        @ApiResponse(
                responseCode = "400",
                description = "Invalid request body",
                content = @Content(schema = @Schema(implementation = ValidationErrorResponse.class))),
        @ApiResponse(
                responseCode = "409",
                description = "Duplicate tag name",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/api/v1/tags")
    ResponseEntity<TagResponse> createTag(
            @Parameter(description = "Tag payload", required = true)
            @Valid @RequestBody TagRequest request
    );

    @Operation(summary = "Patch tag", description = "Partially updates a tag.")
    @ApiResponses(value = {
        @ApiResponse(
                responseCode = "200",
                description = "Tag updated successfully",
                content = @Content(schema = @Schema(implementation = TagResponse.class))),
        @ApiResponse(
                responseCode = "400",
                description = "Invalid request body",
                content = @Content(schema = @Schema(implementation = ValidationErrorResponse.class))),
        @ApiResponse(
                responseCode = "404",
                description = "Tag not found",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(
                responseCode = "409",
                description = "Duplicate tag name",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PatchMapping("/api/v1/tags/{id}")
    ResponseEntity<TagResponse> updateTag(
            @Parameter(description = "Tag ID", required = true, example = "1")
            @PathVariable Long id,
            @Parameter(description = "Partial tag payload", required = true)
            @Valid @RequestBody TagUpdateRequest request
    );

    @Operation(summary = "Delete tag", description = "Deletes a tag by ID.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Tag deleted successfully"),
        @ApiResponse(
                responseCode = "404",
                description = "Tag not found",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @DeleteMapping("/api/v1/tags/{id}")
    ResponseEntity<Void> deleteTag(
            @Parameter(description = "Tag ID", required = true, example = "1")
            @PathVariable Long id
    );
}
