package com.finance.tracker.service.impl;

import com.finance.tracker.domain.Tag;
import com.finance.tracker.dto.request.TagRequest;
import com.finance.tracker.dto.response.TagResponse;
import com.finance.tracker.mapper.TagMapper;
import com.finance.tracker.repository.TagRepository;
import com.finance.tracker.service.TagService;

import java.util.List;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
@Slf4j
public class TagServiceImpl implements TagService {

    private final TagRepository tagRepository;
    private final TagMapper tagMapper;

    @Override
    public TagResponse getTagById(Long id) {
        Tag tag = tagRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tag not found " + id));
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
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Tag with name '" + request.getName() + "' already exists");
        }
        Tag tag = tagMapper.fromRequest(request);
        Tag saved = tagRepository.save(tag);
        return tagMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public TagResponse updateTag(Long id, TagRequest request) {
        Tag tag = tagRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tag not found " + id));
        if (request.getName() != null) {
            String normalizedName = tagMapper.normalizeName(request.getName());
            if (tagRepository.existsByNameAndIdNot(normalizedName, id)) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Tag with name '" + request.getName() + "' already exists");
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
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Tag not found " + id);
        }
        tagRepository.deleteById(id);
    }
}
