package com.finance.tracker.controller;

import com.finance.tracker.controller.api.TagControllerApi;
import com.finance.tracker.dto.request.TagRequest;
import com.finance.tracker.dto.request.TagUpdateRequest;
import com.finance.tracker.dto.response.TagResponse;
import com.finance.tracker.service.TagService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class TagController implements TagControllerApi {

    private final TagService service;

    public ResponseEntity<TagResponse> getTagById(@PathVariable("id") Long id) {
        return ResponseEntity.ok(service.getTagById(id));
    }

    public ResponseEntity<List<TagResponse>> getAllTags() {
        return ResponseEntity.ok(service.getAllTags());
    }

    public ResponseEntity<TagResponse> createTag(@Valid @RequestBody TagRequest request) {
        TagResponse response = service.createTag(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    public ResponseEntity<TagResponse> updateTag(@PathVariable("id") Long id,
            @Valid @RequestBody TagUpdateRequest request) {
        return ResponseEntity.ok(service.updateTag(id, request));
    }

    public ResponseEntity<Void> deleteTag(@PathVariable("id") Long id) {
        service.deleteTag(id);
        return ResponseEntity.noContent().build();
    }
}
