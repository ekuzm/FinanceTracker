package com.finance.tracker.service;

import com.finance.tracker.dto.request.TagRequest;
import com.finance.tracker.dto.request.TagUpdateRequest;
import com.finance.tracker.dto.response.TagResponse;

import java.util.List;

public interface TagService {
    TagResponse getTagById(Long id);

    List<TagResponse> getAllTags();

    TagResponse createTag(TagRequest request);

    TagResponse updateTag(Long id, TagUpdateRequest request);

    void deleteTag(Long id);
}
