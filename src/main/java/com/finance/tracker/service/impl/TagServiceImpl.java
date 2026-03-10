package com.finance.tracker.service.impl;

import com.finance.tracker.domain.Tag;
import com.finance.tracker.dto.request.TagRequest;
import com.finance.tracker.dto.request.TagUpdateRequest;
import com.finance.tracker.dto.response.TagResponse;
import com.finance.tracker.exception.DuplicateResourceException;
import com.finance.tracker.exception.ResourceNotFoundException;
import com.finance.tracker.mapper.TagMapper;
import com.finance.tracker.repository.TagRepository;
import com.finance.tracker.service.TagService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class TagServiceImpl implements TagService {

    private final TagRepository tagRepository;
    private final TagMapper tagMapper;

    @Override
    public TagResponse getTagById(Long id) {
        Tag tag = tagRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tag not found " + id));
        return tagMapper.toResponse(tag);
    }

    @Override
    public List<TagResponse> getAllTags() {
        return tagRepository.findAll().stream()
                .map(tagMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public TagResponse createTag(TagRequest request) {
        String normalizedName = tagMapper.normalizeName(request.getName());
        if (tagRepository.existsByName(normalizedName)) {
            throw new DuplicateResourceException("Tag with name '" + request.getName() + "' already exists");
        }
        Tag tag = tagMapper.fromRequest(request);
        Tag saved = tagRepository.save(tag);
        return tagMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public TagResponse updateTag(Long id, TagUpdateRequest request) {
        Tag tag = tagRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tag not found " + id));
        if (request.getName() != null) {
            String normalizedName = tagMapper.normalizeName(request.getName());
            if (tagRepository.existsByNameAndIdNot(normalizedName, id)) {
                throw new DuplicateResourceException("Tag with name '" + request.getName() + "' already exists");
            }
            tag.setName(normalizedName);
        }
        Tag saved = tagRepository.save(tag);
        return tagMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public void deleteTag(Long id) {
        if (!tagRepository.existsById(id)) {
            throw new ResourceNotFoundException("Tag not found " + id);
        }
        tagRepository.deleteById(id);
    }
}
